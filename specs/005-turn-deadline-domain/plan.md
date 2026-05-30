# Implementation Plan: Deadline de turno como concepto de dominio

**Branch**: `005-turn-deadline-domain` | **Date**: 2026-05-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-turn-deadline-domain/spec.md`

## Summary

Exponer la cuenta regresiva del turno (instante de vencimiento, duración y asiento que debe actuar)
a jugadores y espectadores, garantizando que **lo mostrado es exactamente lo que se ejecuta** como
forfeit.

Decisión de diseño central (confirmada por el usuario): el deadline se modela como **proyección
derivada**, no como evento transicional. Es decir:

- El **ancla temporal** (cuándo empezó la ventana de "debe actuar") y el **asiento que debe actuar**
  son conocimiento de dominio.
- La **duración** del plazo es configuración operativa de aplicación (FR-010).
- El **deadline absoluto** = `ancla + duración`, una proyección computada con una única fórmula,
  reutilizada por snapshot, evento en vivo, scheduler de ejecución y reconciliación al reiniciar.
- Los eventos `ACTION_DEADLINE_SET` / `ACTION_DEADLINE_CLEARED` se emiten como **eventos derivados**
  (no avanzan `stateVersion`), igual que `AVAILABLE_ACTIONS_UPDATED` y `PLAYER_HAND_UPDATED`.

El proyecto **ya tiene la infraestructura clave**: el ancla `lastActivityAt` existe y se persiste
(columna creada en `V2__add_last_activity_at.sql`); el marcador `MatchDerivedEvent` y el ruteo que
asigna `stateVersion = null` a los derivados ya existen
([MatchNotificationEventTranslator.java:53](../../src/main/java/com/villo/truco/application/eventhandlers/MatchNotificationEventTranslator.java)).
Esto permite implementar la feature **sin migración de base de datos** y reutilizando patrones
existentes.

## Technical Context

**Language/Version**: Java 21 (toolchain `JavaLanguageVersion.of(21)`)

**Primary Dependencies**: Spring Boot 4.0.3 (web, websocket, data-jpa, flyway, validation,
security), STOMP sobre WebSocket.

**Storage**: PostgreSQL (producción), H2 en modo PostgreSQL (tests). Flyway para migraciones.
**No se requiere migración nueva**: se reutiliza la columna `last_activity_at` existente.

**Testing**: JUnit (Gradle `test`), H2 en memoria (sin Docker), ArchUnit (`CleanArchitectureTest`),
JaCoCo (mínimo 70% de líneas). Integración existente reutilizable: `TimeoutExactnessMatchIT`,
`TimeoutReconciliationIT`.

**Target Platform**: Servicio backend (JVM).

**Project Type**: Web service backend (Clean/Hexagonal + DDD). Single project.

**Performance Goals**: El instante de forfeit ejecutado coincide con el deadline expuesto dentro de
1 segundo (SC-001). No introduce carga adicional relevante: el deadline es un cálculo aritmético
sobre estado ya cargado.

**Constraints**: Dominio puro (sin Spring/JPA). El tiempo entra al dominio como **valor** (`Instant
now`), nunca como capacidad (`Clock` inyectado al agregado). Un único deadline autoritativo
(FR-003): prohibido tener un valor "mostrado" distinto del "ejecutado".

**Scale/Scope**: Alcance acotado a la partida (match). Liga/copa solo se afectan indirectamente vía
el forfeit ya existente.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificación                                                                                                                                                                                                                                                                             |
|---------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | ✅ PASS | Cambios respetan `Domain → Application → Infrastructure`. El dominio emite el evento derivado y posee ancla+asiento; la app proyecta el deadline; infra expone DTOs. Sin imports cruzados entre agregados. No se agregan endpoints (se reutilizan `GET /api/matches/{id}` y `/spectate`). |
| **II. Dominio Puro**      | ✅ PASS | El dominio recibe `Instant now` como parámetro de valor en los métodos de acción; no se inyecta `Clock` ni Spring al agregado. La duración (config operativa) **no** entra al dominio: el evento derivado lleva el ancla y el asiento; la app compone `ancla + duración`.                 |
| **III. Test-First + 70%** | ✅ PASS | Tests de dominio (cada transición resetea ancla y emite evento derivado), de assembler, de translator e integración (extender `TimeoutExactnessMatchIT`). Títulos en español.                                                                                                             |
| **IV. Español**           | ✅ PASS | Todos los artefactos de este feature en español.                                                                                                                                                                                                                                          |
| **V. YAGNI**              | ✅ PASS | Sin migración (reutiliza `last_activity_at`), sin endpoints nuevos, sin almacenar un deadline absoluto redundante, reutiliza el mecanismo `MatchDerivedEvent` existente.                                                                                                                  |

**Resultado**: PASS. No hay violaciones que justificar; la sección Complexity Tracking se omite.

## Project Structure

### Documentation (this feature)

```text
specs/005-turn-deadline-domain/
├── plan.md              # Este archivo
├── research.md          # Decisiones de diseño (Fase 0)
├── data-model.md        # Modelo conceptual del deadline (Fase 1)
├── quickstart.md        # Cómo verificar la feature (Fase 1)
├── contracts/           # Contrato expuesto: shapes de snapshot y eventos (Fase 1)
│   └── deadline-contract.md
└── tasks.md             # (NO lo crea /speckit-plan; lo crea /speckit-tasks)
```

### Source Code (repository root)

Archivos impactados (existentes salvo donde se indica "nuevo"):

```text
src/main/java/com/villo/truco/
├── domain/model/match/
│   ├── Match.java                              # set ancla en transiciones must-act; emitir evento derivado
│   ├── Round.java                              # confirmar/exponer "asiento que debe actuar" (canto pendiente)
│   └── events/
│       ├── ActionDeadlineSetEvent.java         # NUEVO (implements MatchDerivedEvent)
│       └── ActionDeadlineClearedEvent.java     # NUEVO (implements MatchDerivedEvent)
├── application/
│   ├── eventhandlers/
│   │   ├── MatchEventMapper.java               # componer deadline = ancla + idleTimeout (inyectar duración); payload de los 2 eventos
│   │   ├── MatchNotificationEventTranslator.java # SIN cambios de composición (línea 53 ya pone stateVersion=null a derivados)
│   │   ├── SpectatorNotificationEventTranslator.java # SIN cambios (reenvía todo lo que no es SeatTargetedEvent)
│   │   ├── MatchRecipientResolver.java          # SIN cambios (entrega a ambos jugadores lo que no es SeatTargetedEvent)
│   │   └── MatchTimeoutEventHandler.java        # reloj por estado: IN_PROGRESS desde ACTION_DEADLINE_SET; pre-juego como hoy (fuente única, D7)
│   ├── assemblers/
│   │   ├── MatchStateDTOAssembler.java          # +3 campos en RoundStateDTO
│   │   └── SpectatorMatchStateDTOAssembler.java # +3 campos en SpectatorRoundStateDTO
│   └── dto/
│       ├── RoundStateDTO.java                   # +actionDeadline, +turnDurationMillis, +actionDeadlineSeat
│       └── SpectatorRoundStateDTO.java          # idem
├── infrastructure/http/dto/response/
│   ├── RoundStateResponse.java                  # +3 campos
│   └── SpectatorRoundStateResponse.java         # +3 campos
└── infrastructure/scheduler/
    └── MatchTimeoutReconciliationSource.java    # ya usa lastActivityAt + idleTimeout (verificar alineación)

src/test/java/com/villo/truco/                   # tests por capa (ver tasks)
docs/CONTRATOS_API.md                            # CORREGIR §9.5: mover los 2 eventos a "derivados" (sin stateVersion)
```

**Structure Decision**: Single project, layout hexagonal existente. El feature toca las tres capas
con cambios quirúrgicos; el núcleo del trabajo está en `domain/model/match` (ancla + evento
derivado) y `application/eventhandlers` (proyección + fuente única del scheduler).

## Fase 0 — Research

Ver [research.md](research.md). Resuelve las decisiones clave: proyección vs estado absoluto;
ubicación de la duración; inyección de `now`; alineación scheduler/snapshot/reconciliación;
semántica de "asiento que debe actuar" ante canto pendiente; y la corrección al contrato
documentado.

## Fase 1 — Design & Contracts

- [data-model.md](data-model.md): el deadline como proyección (ancla de dominio + duración de
  config), sin nueva persistencia.
- [contracts/deadline-contract.md](contracts/deadline-contract.md): shapes exactos de los 3 campos
  en
  snapshot (jugador y espectador) y de los eventos derivados `ACTION_DEADLINE_SET` /
  `ACTION_DEADLINE_CLEARED`, más la corrección a `docs/CONTRATOS_API.md`.
- [quickstart.md](quickstart.md): pasos de verificación manual y automatizada.
- Agent context: se actualiza la referencia de plan en `CLAUDE.md`.

## Documentación a actualizar (gate de cierre)

- **`docs/CONTRATOS_API.md`**:
    - §9.5: **mover** `ACTION_DEADLINE_SET` y `ACTION_DEADLINE_CLEARED` de la lista de "Eventos
      transicionales (consumen `stateVersion`)" a una clasificación de **derivados** (no avanzan
      `stateVersion`), coherente con `AVAILABLE_ACTIONS_UPDATED` / `PLAYER_HAND_UPDATED`. Esta es la
      consecuencia directa de la decisión de proyección derivada y **debe** reflejarse o el contrato
      con FE queda falso.
    - §4.18, §4.14, §4.15, §9.6: ya redactadas anticipadamente; verificar que coincidan con los
      shapes finales (en particular, que los eventos no aparezcan como portadores de
      `stateVersion`).
- **`README.md`**: evaluar mención del temporizador de turno visible como capacidad del sistema.
