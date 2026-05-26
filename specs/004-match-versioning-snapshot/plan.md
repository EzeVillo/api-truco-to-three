# Implementation Plan: Versionado de Match para Reconciliación Snapshot + Stream

**Branch**: `004-match-versioning-snapshot` | **Date**: 2026-05-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-match-versioning-snapshot/spec.md`

## Summary

Introducir un contador de **transiciones** (`stateVersion`) por agregado `Match`, monotónicamente
creciente y persistente, que se incrementa **exactamente en uno por cada evento de dominio
transicional emitido por el aggregate**. Ese número viaja en (a) el snapshot REST que devuelve
`GET /api/matches/{matchId}` y (b) cada evento publicado por `/user/queue/match`. El cliente lo
usa como cursor único para reconciliar snapshot + stream: descarta cualquier evento con
`stateVersion <= snapshot.stateVersion`, detecta huecos cuando recibe `stateVersion > último + 1`,
y trata duplicados (`stateVersion == último`) como no-op.

Las notificaciones por jugador que **no** son transiciones del aggregate
(p. ej. `PLAYER_HAND_UPDATED`, `AVAILABLE_ACTIONS_UPDATED`, eventos de chat) NO consumen
`stateVersion` y se entregan por un canal distinguible (`/user/queue/match-derived`,
nombre tentativo) para que el cliente no las confunda con la secuencia transicional.

Los eventos con información oculta (reparto) se modelan como **una sola transición**
que produce **una variante de payload por jugador** con el mismo `stateVersion` y mismo
`eventType`, pero cartas filtradas por destinatario.

## Technical Context

**Language/Version**: Java 21 (toolchain del proyecto)

**Primary Dependencies**: Spring Boot 3.x, Spring WebSocket/STOMP, Spring Data JPA, Hibernate,
PostgreSQL driver, Jackson, ArchUnit, JaCoCo.

**Storage**: PostgreSQL en producción (vía Flyway). H2 en modo PostgreSQL para tests
(`create-drop`, sin Flyway). El campo `version` JPA ya existe en `MatchJpaEntity` pero se usa
para optimistic locking y se incrementa **una vez por `save`**, no por transición — no sirve
como `stateVersion`. Hay que persistir un contador adicional.

**Testing**: JUnit 5 + AssertJ. Tests de dominio sin Spring. Tests de integración con
`@SpringBootTest` y `@AutoConfigureMockMvc`. ArchUnit (`CleanArchitectureTest`). JaCoCo 70%
mínimo.

**Target Platform**: Servidor JVM (Linux/Windows). Clientes web/móvil que consumen REST + STOMP.

**Project Type**: Web service single-module (Gradle).

**Performance Goals**: Sin cambios de presupuesto respecto del estado actual. El incremento del
contador es una operación in-memory O(1) por evento y un campo `long` adicional a persistir
junto con el aggregate.

**Constraints**:

- Compatibilidad hacia atrás: el snapshot REST agrega el campo `stateVersion` sin romper clientes
  que no lo lean (FR-012).
- Mismo `stateVersion` para todas las variantes por jugador de un mismo evento de información
  oculta (FR-005).
- El contador NO debe retroceder en reinicios ni reasignarse (FR-010): persistencia en el mismo
  row del match.
- Las notificaciones derivadas (turn-derived, available actions) NO deben consumir el contador
  ni mezclarse con la cola transicional (FR-006). Esto implica reclasificar
  `PlayerHandUpdatedEvent`, `AvailableActionsUpdatedEvent` y eventualmente `TurnChangedEvent`
  como derivados, no transicionales. Ver decisión D-04 en research.md.
- El payload redactado no debe filtrar info indirecta (FR-011).

**Scale/Scope**: Eventos por match típicos del orden de 10²–10³ (cartas, cantos, scores, fin de
manos). `long` es ampliamente suficiente.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                          | Gate                                                                                                                                                                                                                                                                                                                         | Estado |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| I. Arquitectura Hexagonal + DDD    | `stateVersion` vive en el agregado `Match` (domain). Persistencia en `MatchJpaEntity` (infrastructure). El envoltorio que añade `stateVersion` a los eventos publicados se hace en application (en el flujo de publicación de eventos). Ningún cambio cruza capas en el sentido prohibido.                                   | ✅      |
| II. Dominio Puro                   | Se agrega un `long stateVersion` y un método `nextStateVersion()` en `Match`/`AggregateBase` (Java puro). Sin Spring, sin JPA en domain.                                                                                                                                                                                     | ✅      |
| III. Test-First con Coverage       | Plan obliga a tests de dominio (Match.stateVersion), tests de aplicación (envoltorio de versión + redacción por jugador) y tests de integración REST/WS verificando el campo. Coverage 70% mantenible.                                                                                                                       | ✅      |
| IV. Español como Idioma de Trabajo | spec/plan/research/data-model/contratos/quickstart/tasks/checklists en español. Títulos de tests en español.                                                                                                                                                                                                                 | ✅      |
| V. Simplicidad / YAGNI             | Estrategia A (una sola secuencia). No se implementa replay de eventos desde origen ni event sourcing, sólo un contador monotónico + canal aparte para derivados. Sin abstracciones genéricas tipo "versionable aggregate"; se aplica sólo a Match. Otros bounded contexts adoptarán el patrón cuando lo necesiten, no ahora. | ✅      |

**Resultado**: PASS. No hay violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/004-match-versioning-snapshot/
├── plan.md              # Este archivo
├── spec.md              # Spec funcional ya generado
├── research.md          # Decisiones técnicas (Phase 0)
├── data-model.md        # Entidades y campos modificados (Phase 1)
├── quickstart.md        # Cómo verificar manualmente la feature (Phase 1)
├── contracts/           # Contratos REST/WS afectados (Phase 1)
│   ├── rest-match-snapshot.md
│   └── ws-match-stream.md
├── checklists/          # Generados por /speckit-checklist (si aplica)
└── tasks.md             # Generado por /speckit-tasks (no aquí)
```

### Source Code (repository root)

Estructura monolítica del módulo, ya existente. Archivos clave que toca esta feature:

```text
src/main/java/com/villo/truco/
├── domain/
│   ├── shared/
│   │   ├── AggregateBase.java                       # (modificar) agregar stateVersion
│   │   └── DomainEventBase.java                     # (modificar opcional) doc del campo timestamp
│   └── model/match/
│       ├── Match.java                               # (modificar) incrementar stateVersion al agregar cada evento transicional
│       ├── MatchSnapshot.java                       # (modificar) agregar stateVersion
│       ├── MatchSnapshotExtractor.java              # (modificar) propagar stateVersion
│       └── events/
│           ├── MatchDomainEvent.java                # (modificar) campo stateVersion + getter
│           ├── MatchEventEnvelope.java              # (modificar) propaga stateVersion al inner
│           └── (clasificación derivada/transicional — ver research D-04)
├── application/
│   ├── dto/MatchStateDTO.java                       # (modificar) agregar stateVersion
│   ├── assemblers/MatchStateDTOAssembler.java       # (modificar) leer y propagar stateVersion
│   ├── events/MatchEventNotification.java           # (modificar) agregar stateVersion (nullable para derivados)
│   └── eventhandlers/
│       ├── MatchNotificationEventTranslator.java    # (modificar) split transicional vs derivado, asigna stateVersion
│       ├── MatchEventMapper.java                    # (sin cambios estructurales; usa payload existente)
│       └── (posible nuevo) MatchDerivedNotificationTranslator.java   # canal derivado
└── infrastructure/
    ├── persistence/
    │   ├── entities/MatchJpaEntity.java             # (modificar) agregar columna state_version
    │   └── mappers/MatchMapper.java                 # (modificar) propagar stateVersion en ambas direcciones
    ├── http/dto/response/MatchStateResponse.java    # (modificar) exponer stateVersion
    └── websocket/
        ├── dto/MatchWsEvent.java                    # (modificar) agregar stateVersion (nullable para derivados)
        └── StompMatchNotificationHandler.java       # (modificar) destino /queue/match para transicional, /queue/match-derived para derivados

src/main/resources/db/migration/
└── VXXX__add_match_state_version.sql                # (nuevo) ALTER TABLE matches ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0

src/test/java/com/villo/truco/
├── domain/model/match/MatchStateVersionTest.java                # (nuevo) tests de invariantes del contador
├── application/eventhandlers/MatchNotificationEventTranslatorTest.java  # (modificar/nuevo) verifica stateVersion propagado, redacción por jugador con mismo stateVersion
├── infrastructure/http/MatchControllerSnapshotVersionTest.java  # (nuevo) GET snapshot incluye stateVersion
└── infrastructure/websocket/MatchWsStreamVersionTest.java       # (nuevo) eventos transicionales avanzan stateVersion estrictamente; derivados van por otro destino
```

**Structure Decision**: Single project (módulo Gradle existente). No se introducen nuevos
submódulos. Todas las rutas anteriores son relativas al raíz del repo.

## Phase 0 — Outline & Research

Ver `research.md` para decisiones técnicas resueltas:

- **D-01** Dónde vive el contador (`Match` aggregate, no JPA `@Version`).
- **D-02** Tipo del contador (`long`, no `int`).
- **D-03** Cuándo se incrementa (al añadir cada evento transicional; los `EventEnvelope` reciben
  el mismo número que su evento interno).
- **D-04** Qué eventos cuentan como transicionales vs derivados (lista cerrada).
- **D-05** Cómo se entrega el canal de derivados (nuevo destino STOMP, sin cambiar contrato del
  destino transicional más allá del campo nuevo).
- **D-06** Redacción de payload por jugador con mismo `stateVersion` (un único evento de dominio
  cuya proyección a payload depende del destinatario).
- **D-07** Persistencia y migración (columna `state_version BIGINT NOT NULL DEFAULT 0`).
- **D-08** Backward compatibility (campo aditivo; el cliente viejo sigue funcionando).
- **D-09** Espectadores ven `stateVersion` también.
- **D-10** Notificaciones de chat siguen en `/user/queue/chat`, fuera de scope.

**Output**: research.md (creado en este commit).

## Phase 1 — Design & Contracts

### data-model.md (entregable)

Documenta:

- `Match` (agregado): nuevo campo `stateVersion: long`, regla de incremento.
- `MatchSnapshot`: nuevo campo `stateVersion`.
- `MatchDomainEvent`: nuevo campo `stateVersion` (sólo poblado tras añadir el evento al
  aggregate).
- Tabla `matches`: nueva columna `state_version BIGINT NOT NULL`.
- Distinción **Transicional vs Derivado** documentada como tabla.

### contracts/ (entregables)

- `rest-match-snapshot.md` — diff del shape de `GET /api/matches/{matchId}` y
  `GET /api/matches/{matchId}/spectate`.
- `ws-match-stream.md` — nuevo campo `stateVersion` en eventos de `/user/queue/match`, y nuevo
  destino `/user/queue/match-derived` para notificaciones que no consumen `stateVersion`.

### quickstart.md (entregable)

Procedimiento manual para reproducir las US 1–4:

1. Levantar dependencias (`docker compose up -d`) y arrancar (`./gradlew bootRun`).
2. Crear un match (`POST /api/matches`), demorar la suscripción y reproducir las US.
3. Comandos `curl`/`wscat` listos para copiar y pegar.

### Agent context update

Actualizar el bloque entre `<!-- SPECKIT START -->` y `<!-- SPECKIT END -->` de
`CLAUDE.md` para apuntar a `specs/004-match-versioning-snapshot/plan.md`.

**Output**: data-model.md, contracts/*, quickstart.md, CLAUDE.md actualizado.

## Documentación a actualizar (post-implementación)

- **README.md**: agregar en la sección de capacidades / WebSocket que cada evento de
  `/user/queue/match` incluye `stateVersion`, y que existe `/user/queue/match-derived` para
  notificaciones por jugador que no avanzan la secuencia. Mencionar `stateVersion` en el snapshot
  REST.
- **docs/CONTRATOS_API.md**:
    - Sección 9.4 "Forma del evento WS / Match": agregar campo `stateVersion` al ejemplo.
    - Sección 9.5 "eventType posibles - Match": agregar subsección "Notificaciones derivadas
      (no avanzan stateVersion)" y mover allí `PLAYER_HAND_UPDATED`, `AVAILABLE_ACTIONS_UPDATED`
      (y los que correspondan tras D-04).
    - Sección de `GET /api/matches/{matchId}`: agregar `stateVersion` al shape de
      `MatchStateResponse`.
    - Sección de `GET /api/matches/{matchId}/spectate`: idem `SpectatorMatchStateResponse`.
    - Documentar la regla cliente para reconciliar snapshot + stream usando `stateVersion`.

## Complexity Tracking

> No hay violaciones a la constitution que justificar.
