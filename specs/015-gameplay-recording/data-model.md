# Data Model: Recopilación de partidas para entrenamiento de bots

**Feature**: 015-gameplay-recording | **Date**: 2026-06-15

## Entidades de dominio (Java puro, `com.villo.truco.domain`)

### `RecordedDecision` (VO, `domain.model.gameplay`)

Representa una decisión jugable registrada. Inmutable.

| Campo | Tipo | Notas |
|-------|------|-------|
| `matchId` | `MatchId` | Partida a la que pertenece. |
| `stateVersion` | `long` | Versión resultante de la transición; cursor de orden por partida. |
| `gameNumber` | `int` | Número de juego dentro de la serie (de `MatchSnapshot`). |
| `roundNumber` | `int` | Número de mano/ronda (de `MatchSnapshot`). |
| `actorSeat` | `ActorSeat` | `PLAYER_ONE` / `PLAYER_TWO`. |
| `actorType` | `ActorType` | `HUMAN` / `BOT`. |
| `action` | `RecordedAction` | Acción concreta tomada. |
| `snapshot` | `MatchSnapshot` | Estado completo y sin redactar tras la transición. |
| `occurredAt` | `Instant` | Momento de la decisión. |
| `schemaVersion` | `int` | Versión del esquema de captura (inicial = 1). |

**Reglas/invariantes**:
- Todos los campos no nulos (la acción siempre corresponde a uno de los 6 tipos).
- El VO no contiene lógica de juego; es un contenedor de datos para el puerto de salida.
- Reutiliza `MatchSnapshot` existente (no se redefine el estado).

### `RecordedAction` (VO, `domain.model.gameplay`)

| Campo | Tipo | Notas |
|-------|------|-------|
| `type` | `RecordedActionType` | Discriminador. |
| `detail` | detalle mínimo | Carta jugada / canto / respuesta, según `type`; nulo para acciones sin parámetro (p. ej. `FOLD`, `CALL_TRUCO`). |

> El detalle puede modelarse con los VOs de dominio ya existentes (`Card`, respuestas de truco/envido,
> tipo de canto de envido). La infraestructura lo serializa a JSONB en `action_detail`.

### Enums (`domain.model.gameplay`)

- `RecordedActionType`: `PLAY_CARD`, `CALL_TRUCO`, `RESPOND_TRUCO`, `CALL_ENVIDO`, `RESPOND_ENVIDO`,
  `FOLD`.
- `ActorType`: `HUMAN`, `BOT`.
- `ActorSeat`: `PLAYER_ONE`, `PLAYER_TWO`.

## Puerto de salida (`com.villo.truco.domain.ports`)

### `GameplayRecorderPort`

```java
public interface GameplayRecorderPort {
  void record(RecordedDecision decision);
}
```

- Implementado por `JpaGameplayRecorderAdapter` (infrastructure).
- Contrato: persiste la decisión de forma append-only e idempotente por `(matchId, stateVersion)`.
  Las excepciones pueden propagar; el **decorator** las traga y loguea (FR-010).

## Apoyo en application

### `MatchActionCommand` (marker, `application.commands`)

```java
public interface MatchActionCommand {
  MatchId matchId();
  PlayerId playerId();
}
```

- Implementada por los 6 commands existentes (`PlayCardCommand`, `CallTrucoCommand`,
  `RespondTrucoCommand`, `CallEnvidoCommand`, `RespondEnvidoCommand`, `FoldCommand`) — ya exponen
  `matchId()` y `playerId()`; solo se agrega `implements MatchActionCommand`.

### `RecordedActionFactory` (`application.usecases.recording`)

- Mapea un `MatchActionCommand` concreto → `RecordedAction` (switch por tipo, pattern matching).

## Persistencia (infrastructure) — tabla `match_action_log`

> Migración: **`V22__create_match_action_log.sql`** (la última es
> `V21__campaign_unlocked_casual_bots.sql`; **re-verificar el número contra la branch** antes de
> crear el archivo, por si entra otra migración).

| Columna | Tipo (PostgreSQL) | Restricciones |
|---------|-------------------|---------------|
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | PK |
| `match_id` | `UUID` | NOT NULL |
| `state_version` | `BIGINT` | NOT NULL |
| `game_number` | `INT` | NOT NULL |
| `round_number` | `INT` | NOT NULL |
| `actor_seat` | `VARCHAR(16)` | NOT NULL (`PLAYER_ONE`/`PLAYER_TWO`) |
| `actor_type` | `VARCHAR(8)` | NOT NULL (`HUMAN`/`BOT`) |
| `action_type` | `VARCHAR(24)` | NOT NULL |
| `action_detail` | `JSONB` | NULL (detalle de la acción) |
| `match_state` | `JSONB` | NOT NULL (snapshot completo, sin redactar) |
| `schema_version` | `INT` | NOT NULL |
| `occurred_at` | `TIMESTAMPTZ` | NOT NULL |
| `recorded_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` |

**Índices / constraints**:
- `UNIQUE (match_id, state_version)` — orden e idempotencia (FR-006, FR-013).
- Índice implícito por el UNIQUE cubre consultas por partida ordenadas por `state_version`.

**Append-only**: el adapter sólo hace `INSERT ... ON CONFLICT (match_id, state_version) DO NOTHING`.
Sin UPDATE/DELETE en el código de aplicación (FR-007). No se añade trigger de inmutabilidad (YAGNI).

### `MatchActionLogJpaEntity` + `MatchActionLogJpaRepository`

- Entidad JPA mapeando la tabla; `match_state` y `action_detail` como JSONB (mismo enfoque que
  `MatchJpaEntity.current_round`).
- Repositorio Spring Data para el `INSERT` (idempotente).

## Flujo de datos (resumen)

```
Controller / ExecuteBotTurnCommandHandler
        │  (mismos 6 beans de puerto de entrada)
        ▼
GameplayRecordingDecorator.handle(command)            [application, FUERA del pipeline]
        │  1) matchId = delegate.handle(command)       → retryTransactionalPipeline → handler  [COMMIT]
        │  2) match = MatchQueryRepository.findById(matchId)        [post-commit, lectura fresca]
        │  3) snapshot = MatchSnapshotExtractor.extract(match)      → stateVersion, estado completo
        │  4) actorType = BotRegistry.isBot(playerId); actorSeat por comparación
        │  5) action = RecordedActionFactory.from(command)
        │  6) try { GameplayRecorderPort.record(RecordedDecision...) } catch { log & continue }
        ▼
JpaGameplayRecorderAdapter.record(...)                [infrastructure, @Transactional nueva tx]
        ▼
INSERT ... ON CONFLICT DO NOTHING  →  match_action_log
```

## Cómo se consume (sin tooling, por SQL)

Reconstruir una partida en orden:

```sql
SELECT state_version, actor_seat, actor_type, action_type, action_detail, match_state
FROM match_action_log
WHERE match_id = :matchId
ORDER BY state_version;
```

El par de entrenamiento "(estado previo, acción)" se obtiene tomando `match_state` de la fila con
`state_version = v` como estado-de-decisión y `action_type/action_detail` de la fila `v+1` como la
acción tomada en ese estado.
