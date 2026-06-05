# Implementation Plan: Notificaciones post-commit (fin de race conditions A y B)

**Branch**: `010-post-commit-events` | **Date**: 2026-06-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-post-commit-events/spec.md`

## Summary

Hoy todas las notificaciones en tiempo real al usuario se emiten **dentro** de la transacción que
produjo el cambio, salvo 5 eventos que ya implementan `PostCommitApplicationEvent`. Esto provoca
race conditions: lectura prematura con 404 (categoría A, p. ej. partida rápida) y avisos
fantasma/duplicados ante rollback o reintento (categoría B, p. ej. chat).

El enfoque técnico es **reutilizar el mecanismo existente** (`PostCommitApplicationEvent` +
`TransactionalApplicationEventPublisher`, que ya difiere a `afterCommit`): marcar como post-commit
los application events de notificación que hoy no lo están, manteniendo in-transaction los eventos
de
coordinación entre dominios que disparan escrituras atómicas (categoría C). Como salvaguarda
("que no nos vuelva a pasar") se introduce un marcador complementario
`InTransactionApplicationEvent`
y una regla ArchUnit que obliga a que **todo** `ApplicationEvent` declare explícitamente uno de los
dos marcadores, fallando el build si se omite.

No cambia ningún contrato REST/WebSocket: el contenido, los destinatarios y el orden de los avisos
se
mantienen idénticos; sólo cambia el **momento** de emisión (tras el commit).

## Technical Context

**Language/Version**: Java 21 (Spring Boot)

**Primary Dependencies**: Spring Boot, Spring Tx, Spring Messaging (STOMP/WebSocket), ArchUnit,
JaCoCo

**Storage**: PostgreSQL (producción), H2 PostgreSQL-mode (tests)

**Testing**: JUnit 5 + AssertJ + Mockito; ArchUnit (`CleanArchitectureTest`); H2 en memoria

**Target Platform**: Servidor (JVM)

**Project Type**: Single project — web service (Clean/Hexagonal + DDD)

**Performance Goals**: Sin objetivos nuevos; cambio de timing de emisión, no de volumen

**Constraints**: Respetar layering ArchUnit (Domain → Application → Infrastructure); el dominio
permanece puro; coverage ≥ 70%

**Scale/Scope**: ~12 clases de application event afectadas (marcadores) + 1 regla ArchUnit + tests.
Sin cambios de esquema de datos ni de API.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                          | Estado | Notas                                                                                                                                                         |
|------------------------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| I. Arquitectura Hexagonal + DDD    | ✅ PASS | Los marcadores viven en `application/events`; la lógica de deferral ya vive en `infrastructure`. No se cruzan capas. La regla ArchUnit refuerza el principio. |
| II. Dominio Puro                   | ✅ PASS | No se toca `domain`. Los marcadores son interfaces de la capa application sin dependencias de framework.                                                      |
| III. Test-First + Coverage ≥ 70%   | ✅ PASS | Se agregan tests de timing (post-commit/rollback/retry) y la regla ArchUnit. Títulos en español.                                                              |
| IV. Español como idioma de trabajo | ✅ PASS | Todos los artefactos en español.                                                                                                                              |
| V. Simplicidad / YAGNI             | ✅ PASS | Se reutiliza el mecanismo existente; el único agregado es un marcador simétrico + una regla. No se introduce infraestructura paralela.                        |

**Resultado**: PASS. Sin violaciones; no se requiere Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/010-post-commit-events/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1 (taxonomía de eventos)
├── quickstart.md        # Fase 1 (cómo verificar)
├── contracts/
│   └── README.md        # Fase 1 (sin cambios de contrato)
└── checklists/
    └── requirements.md  # de /speckit-specify
```

### Source Code (repository root)

```text
src/main/java/com/villo/truco/
├── application/events/
│   ├── PostCommitApplicationEvent.java          # existe (marcador post-commit)
│   ├── InTransactionApplicationEvent.java        # NUEVO (marcador in-tx, salvaguarda)
│   ├── MatchEventNotification.java               # → implementa PostCommit
│   ├── LeagueEventNotification.java              # → implementa PostCommit
│   ├── CupEventNotification.java                 # → implementa PostCommit
│   ├── ChatEventNotification.java                # → implementa PostCommit
│   ├── SpectatorMatchEventNotification.java      # → implementa PostCommit
│   ├── SpectatorCountChanged.java               # → implementa PostCommit
│   ├── MatchCompleted.java                       # → implementa InTransaction
│   ├── MatchAbandoned.java                       # → implementa InTransaction
│   ├── MatchForfeited.java                       # → implementa InTransaction
│   └── ResourceBecameUnjoinable.java            # → implementa InTransaction
├── social/application/events/
│   └── SocialEventNotification.java             # → implementa PostCommit
├── profile/application/events/
│   └── ProfileEventNotification.java            # → implementa PostCommit
└── infrastructure/events/
    └── TransactionalApplicationEventPublisher.java  # SIN cambios (ya difiere PostCommit)

src/test/java/com/villo/truco/
├── architecture/ (o donde viva CleanArchitectureTest)
│   └── CleanArchitectureTest.java               # + regla: todo ApplicationEvent declara 1 marcador
├── infrastructure/events/
│   └── TransactionalApplicationEventPublisherTest.java  # + casos post-commit/rollback
└── ... tests de integración de quick match / chat (categoría A y B)
```

**Structure Decision**: Single project existente. El cambio es localizado en `application/events`
(marcadores) y en la suite de tests. La infraestructura de publicación ya soporta el deferral, así
que no se modifica `TransactionalApplicationEventPublisher`.

## Decisiones de diseño (resumen de Phase 0)

1. **Reutilizar `PostCommitApplicationEvent`**: el publisher ya hace `registerSynchronization` +
   `afterCommit` cuando hay tx activa, y publica inmediato si no la hay (cubre FR-007). No se crea
   infraestructura nueva.
2. **Marcador complementario `InTransactionApplicationEvent`**: interfaz vacía que documenta y marca
   los eventos de coordinación que DEBEN quedar in-tx. No altera el comportamiento del publisher
   (sólo `PostCommit` se difiere); sirve para la regla de arquitectura.
3. **Regla ArchUnit (salvaguarda, FR-009)**: toda clase concreta que implemente `ApplicationEvent`
   debe implementar **exactamente uno** de
   `{PostCommitApplicationEvent, InTransactionApplicationEvent}`.
   El build falla si se agrega un evento nuevo sin decidir. Las clases base/marcadores se excluyen.
4. **Asignación de marcadores**:
    - Post-commit (notificaciones a usuario): `MatchEventNotification`, `LeagueEventNotification`,
      `CupEventNotification`, `ChatEventNotification`, `SocialEventNotification`,
      `ProfileEventNotification`, `SpectatorMatchEventNotification`, `SpectatorCountChanged`
      (las ya marcadas siguen igual: `PresenceEventNotification`,
      `PublicMatch/Cup/LeagueLobbyNotification`,
      `BotTurnRequired`).
    - In-transaction (coordinación con escrituras atómicas): `MatchCompleted`, `MatchAbandoned`,
      `MatchForfeited`, `ResourceBecameUnjoinable`.
5. **Atomicidad preservada (categoría C)**: los handlers de competición (Liga/Copa), cleanup de
   espectadores y expiración de invitaciones siguen ejecutando sus escrituras dentro de la tx; sólo
   las **notificaciones derivadas** que publican (que ahora son post-commit) se difieren.

## Documentación a actualizar

- **`CLAUDE.md`**: actualizar la referencia del plan SPECKIT a
  `specs/010-post-commit-events/plan.md`
  (lo hace este comando). Evaluar agregar una línea de convención: "los application events de
  notificación se marcan `PostCommitApplicationEvent`".
- **`docs/CONTRATOS_API.md`**: **sin cambios de shape**. El contenido y los eventTypes WebSocket no
  cambian. Si el documento afirma algo sobre el *timing* de entrega respecto al commit, ajustarlo;
  de lo contrario, no requiere cambios.
- **`README.md`**: sin recurso/capacidad nuevos. No requiere cambios salvo que se quiera documentar
  la convención de emisión post-commit como regla de arquitectura.
- **`.specify/memory/constitution.md`**: opcional — se podría elevar la convención a restricción
  técnica de tiempo real. Queda fuera del alcance directo; se evalúa al cerrar.

## Riesgos y mitigaciones

- **Tests de integración que asumen entrega in-tx**: algunos tests que verifican entrega WS dentro
  de
  la transacción podrían cambiar de timing. Mitigación: revisar tests de integración de quick match,
  chat, liga/copa; ajustarlos para esperar post-commit. Los tests unitarios de los translators no se
  ven afectados (mockean el publisher y sólo verifican que se llamó `publish`).
- **Eventos sin tx activa** (espectadores disparados fuera de tx, si los hubiera): el publisher ya
  publica inmediato → comportamiento correcto, sin retención.
- **Fallo de envío post-commit**: el dato ya está persistido; el fallo se registra vía
  `EventNotifierHealthRegistry` (ya existente). No revierte nada.

```
<!-- (Phase 2 / tasks se generan con /speckit-tasks) -->
```
