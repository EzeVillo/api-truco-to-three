# Implementation Plan: Recopilación de partidas para entrenamiento de bots

**Branch**: `claude/bot-training-match-data-azo9no` | **Date**: 2026-06-15 | **Spec
**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/015-gameplay-recording/spec.md`

## Summary

Registrar de forma **append-only** cada decisión jugable de toda partida (humana y de bot) junto al
**estado completo y sin redactar** de la partida en ese momento, para acumular un dataset perpetuo
con fines de análisis/mejora de bots más adelante.

Enfoque técnico: **NO event sourcing**. Se persiste, por cada acción que produce una transición de
estado, el `MatchSnapshot` resultante (keyado por `stateVersion`) más la acción que la produjo, en
una tabla append-only `match_action_log`. La captura se hace con un **decorator transparente** que
envuelve **por fuera del pipeline transaccional** a los 6 casos de uso de acción compartidos
(`PlayCardUseCase`, `CallTrucoUseCase`, `RespondTrucoUseCase`, `CallEnvidoUseCase`,
`RespondEnvidoUseCase`, `FoldUseCase`). Como tanto el humano como el bot (vía
`ExecuteBotTurnCommandHandler`) ejecutan sus jugadas a través de esos mismos beans, ambos quedan
registrados sin instrumentación específica del bot.

**Fuera de alcance (decisión del usuario)**: no se construye ningún endpoint REST, comando CLI ni
serializador de exportación. El acceso a los datos se hace consultando la tabla directamente por
SQL.
La User Story 2 de la spec queda satisfecha con que los datos sean consultables; no se implementa
tooling de export.

## Technical Context

**Language/Version**: Java 21 (records, sealed, pattern matching switch), Spring Boot.

**Primary Dependencies**: Spring (solo en infrastructure), JPA/Hibernate, Jackson (serialización
JSONB, ya usada por `MatchMapper`), Flyway (migraciones), ArchUnit (gate de arquitectura).

**Storage**: PostgreSQL (producción) / H2 modo PostgreSQL (tests). Nueva tabla append-only
`match_action_log` con dos columnas JSONB (`match_state`, `action_detail`).

**Testing**: JUnit + suite Gradle. Tests de dominio sin Spring/Docker (H2). `CleanArchitectureTest`
(ArchUnit) debe seguir verde. JaCoCo ≥ 70%.

**Target Platform**: Servidor Linux (backend Spring Boot).

**Project Type**: Web service (backend) — Clean/Hexagonal + DDD.

**Performance Goals**: Volumen bajo (un único jugador humano en producción). No hay objetivos de
throughput; la única restricción es no degradar perceptiblemente la latencia de las acciones.

**Constraints**:

- El registro corre **post-commit** y en su **propia transacción**: un fallo de registro NO debe
  revertir ni interrumpir la jugada (FR-010).
- El dominio permanece puro (sin Spring/JPA). Puerto de salida en `domain.ports`.
- No importar agregados entre sí; el registro vive dentro del contexto de match y reutiliza su
  read-model (`MatchSnapshot`), no un agregado externo.

**Scale/Scope**: Decenas/cientos de filas por partida; un jugador real. Sin particionado ni
políticas de retención/expiración en esta iteración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                                              | Estado | Justificación                                                                                                                                   |
|--------------------------------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| I. Hexagonal + DDD (Domain→Application→Infrastructure) | ✅ PASS | Puerto `GameplayRecorderPort` y VOs en domain; decorator + factory en application (Java puro); entidad JPA, adapter y wiring en infrastructure. |
| Controllers dependen de puertos de entrada             | ✅ PASS | No se tocan controllers; el decorator envuelve los beans de puerto de entrada en la `@Configuration`.                                           |
| Agregados no se importan entre sí                      | ✅ PASS | El registro NO es un agregado nuevo ni importa otro agregado; reutiliza el read-model `MatchSnapshot` del propio contexto de match.             |
| II. Dominio puro                                       | ✅ PASS | `GameplayRecorderPort`, `RecordedDecision`, `RecordedAction`, enums: POJOs sin frameworks.                                                      |
| III. Test-first ≥70%                                   | ✅ PASS | Tests unitarios del decorator (incl. swallow de fallo), del factory de acción, del adapter (H2) y de la migración. Títulos en español.          |
| IV. Español en artefactos                              | ✅ PASS | spec/plan/research/data-model en español.                                                                                                       |
| V. Simplicidad / YAGNI                                 | ✅ PASS | Sin endpoint/CLI/export (recortado por el usuario). Un decorator surgical sobre 6 beans; sin abstracciones extra.                               |

**Resultado**: PASS. Sin violaciones — sección *Complexity Tracking* no aplica.

**Desvío respecto del input del usuario (intencional, para respetar la convención real)**: el input
mencionaba `domain/ports/output`. En este repo los puertos de salida viven **planos** en
`com.villo.truco.domain.ports` (p. ej. `MatchRepository`, `MatchEventNotifier`). El nuevo puerto se
ubica ahí: `com.villo.truco.domain.ports.GameplayRecorderPort`.

## Project Structure

### Documentation (this feature)

```text
specs/015-gameplay-recording/
├── plan.md              # Este archivo
├── research.md          # Fase 0: decisiones de diseño
├── data-model.md        # Fase 1: entidades + esquema de tabla
├── quickstart.md        # Fase 1: cómo verificar/consultar
└── checklists/
    └── requirements.md  # Checklist de calidad de la spec (ya creado)
```

> No se genera carpeta `contracts/`: la feature no expone ninguna interfaz externa (sin endpoint,
> sin evento WebSocket nuevo, sin CLI). El único "contrato" es el esquema de la tabla, documentado
> en `data-model.md`.

### Source Code (repository root)

```text
src/main/java/com/villo/truco/
├── domain/
│   ├── ports/
│   │   └── GameplayRecorderPort.java           # NUEVO — puerto de salida
│   └── model/gameplay/                          # NUEVO paquete (VOs del registro)
│       ├── RecordedDecision.java                # VO: snapshot + acción + actor + metadatos
│       ├── RecordedAction.java                  # VO: tipo de acción + detalle
│       ├── RecordedActionType.java              # enum: PLAY_CARD, CALL_TRUCO, ...
│       ├── ActorType.java                       # enum: HUMAN, BOT
│       └── ActorSeat.java                       # enum: PLAYER_ONE, PLAYER_TWO
├── application/
│   ├── commands/
│   │   └── MatchActionCommand.java              # NUEVO — marker iface (matchId(), playerId())
│   │                                            #   implementada por los 6 commands existentes
│   └── usecases/recording/                      # NUEVO paquete
│       ├── GameplayRecordingDecorator.java      # UseCase<C extends MatchActionCommand, MatchId>
│       └── RecordedActionFactory.java           # mapea command -> RecordedAction
└── infrastructure/
    ├── persistence/
    │   ├── entities/MatchActionLogJpaEntity.java        # NUEVO
    │   ├── repositories/MatchActionLogJpaRepository.java# NUEVO (Spring Data)
    │   └── adapters/JpaGameplayRecorderAdapter.java     # NUEVO — implementa el puerto
    └── config/
        └── MatchUseCaseConfiguration.java               # MODIFICADO — envuelve 6 beans

src/main/resources/db/migration/
└── V22__create_match_action_log.sql            # NUEVO (re-verificar número contra la branch)
```

**Structure Decision**: Backend único Hexagonal + DDD existente. El registro se modela como un
**concern transversal del contexto de match** (no un bounded context nuevo): puerto en domain, VOs
en un paquete `domain.model.gameplay`, orquestación en application (decorator), persistencia en
infrastructure.

## Decisiones clave de diseño (detalle en research.md)

1. **Decorator por FUERA del pipeline transaccional** → garantiza semántica post-commit y
   aislamiento. El bean queda:
   `recordingDecorator.decorate( retryTransactionalPipeline.wrap(handler) )`.
   Cuando el decorator corre su lógica de registro, la transacción de la jugada ya commiteó.
2. **El registro se escribe en su propia transacción** (`@Transactional` en el adapter; no hay tx
   activa post-commit, abre una nueva). Si falla, el decorator **traga y loguea** el error (FR-010);
   la jugada ya está commiteada y no se ve afectada.
3. **Re-resolución del estado post-commit**: tras `delegate.handle(command)` (que devuelve el
   `MatchId`), el decorator re-lee la partida vía `MatchQueryRepository` y extrae el `MatchSnapshot`
   con `MatchSnapshotExtractor.extract(...)`. Ese snapshot trae el `stateVersion` resultante (
   cursor)
   y el estado completo (ambas manos, sin redactar).
4. **`actorType` por `BotRegistry.isBot(playerId)`**; **`actorSeat`** comparando `playerId` contra
   `snapshot.playerOne()/playerTwo()`.
5. **Cursor / orden**: `state_version` del snapshot resultante. `UNIQUE(match_id, state_version)` +
   `INSERT ... ON CONFLICT DO NOTHING` da idempotencia y evita duplicados ante reintentos (FR-006,
   FR-013).
6. **`schema_version`** constante por fila (arranca en 1) para sobrevivir cambios de reglas (
   FR-008).
7. **Serialización JSONB** de `MatchSnapshot` y `RecordedAction` reutilizando el `ObjectMapper`
   /patrón
   que ya usa `MatchMapper` para `current_round`.

## Documentación a actualizar

- **`README.md`**: agregar la tabla `match_action_log` y la capacidad "registro de partidas para
  entrenamiento de bots" si el README lista tablas/capacidades/bounded contexts.
- **`docs/CONTRATOS_API.md`**: **no requiere cambios** — no hay endpoint, evento WebSocket ni enum
  expuesto al frontend. (El frontend no se entera de esta feature.)
- **`AGENTS.md`**: opcionalmente, una nota en "Decisiones de dominio y patrones clave" sobre el
  registro post-commit vía decorator externo al pipeline.

```
