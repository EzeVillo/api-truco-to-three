# Data Model — Versionado de Match

Fecha: 2026-05-25

## Aggregate: `Match`

### Campos nuevos

| Campo          | Tipo   | Default | Descripción                                                                                                                                                                                             |
|----------------|--------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `stateVersion` | `long` | `0`     | Contador monotónicamente creciente de transiciones de estado del match. Se incrementa exactamente en uno por cada evento de dominio **transicional** emitido por el aggregate. NO es el `@Version` JPA. |

### Invariantes

- `stateVersion` es monotónicamente creciente: nunca decrece dentro del ciclo de vida de un
  mismo match. (FR-001, FR-010)
- Cada llamada a `addTransitionalEvent(event)` incrementa primero el contador y luego etiqueta
  el evento con el valor resultante. (FR-003)
- `addDerivedEvent(event)` NO incrementa el contador. (FR-006)
- Un `MatchEventEnvelope` que envuelve un evento de `Round` cuenta como una transición y
  consume un único incremento; el envelope y su inner reportan el mismo `stateVersion`.

### Persistencia

- Se persiste en la columna nueva `matches.state_version BIGINT NOT NULL DEFAULT 0`.
- `Match.reconstruct(...)` acepta `stateVersion` como parámetro adicional.
- `MatchMapper.toEntity(...)` y `toDomain(...)` propagan el campo.

---

## Snapshot: `MatchSnapshot`

### Campos nuevos

| Campo          | Tipo   | Descripción                                                                 |
|----------------|--------|-----------------------------------------------------------------------------|
| `stateVersion` | `long` | El `Match.stateVersion` vigente al momento de generar el snapshot. (FR-002) |

`MatchSnapshotExtractor` propaga el campo desde el aggregate.

---

## DTO de aplicación: `MatchStateDTO`

| Campo          | Tipo   | Descripción    |
|----------------|--------|----------------|
| `stateVersion` | `long` | Idem snapshot. |

Análogo en `SpectatorMatchStateDTO` (mismo significado, mismo valor para todos los suscriptores
del mismo match en el mismo instante).

---

## Eventos de dominio

### Campo nuevo en `MatchDomainEvent`

| Campo          | Tipo   | Descripción                                                                                                                                           |
|----------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `stateVersion` | `long` | Número de transición del match **tras** aplicar este evento. Se asigna cuando el evento se añade al aggregate; antes de eso, no tiene valor definido. |

`MatchEventEnvelope` propaga el `stateVersion` del envelope al inner (o el inner lo expone
delegando al envelope).

### Clasificación de eventos

#### Transicionales (consumen `stateVersion`, se entregan a todos los jugadores)

| Evento                                                                                                            | Notas                                                                                                                                                                                                    |
|-------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PlayerJoinedEvent`                                                                                               |                                                                                                                                                                                                          |
| `PlayerReadyEvent`                                                                                                |                                                                                                                                                                                                          |
| `GameStartedEvent`                                                                                                |                                                                                                                                                                                                          |
| `RoundStartedEvent`                                                                                               |                                                                                                                                                                                                          |
| `HandDealtEvent` **(nuevo)**                                                                                      | Reemplaza el rol de reparto que cumplían los dos `PlayerHandUpdatedEvent`. Lleva `Map<PlayerSeat, List<Card>>`. Se materializa en dos variantes de payload con mismo `stateVersion` y mismo `eventType`. |
| `CardPlayedEvent`                                                                                                 |                                                                                                                                                                                                          |
| `TurnChangedEvent`                                                                                                |                                                                                                                                                                                                          |
| `TrucoCalledEvent`, `TrucoRespondedEvent`, `TrucoCancelledByEnvidoEvent`                                          |                                                                                                                                                                                                          |
| `EnvidoCalledEvent`, `EnvidoResolvedEvent`                                                                        |                                                                                                                                                                                                          |
| `FoldedEvent`                                                                                                     |                                                                                                                                                                                                          |
| `HandResolvedEvent`, `RoundEndedEvent`                                                                            |                                                                                                                                                                                                          |
| `ScoreChangedEvent`, `GameScoreChangedEvent`                                                                      |                                                                                                                                                                                                          |
| `MatchFinishedEvent`, `MatchAbandonedEvent`, `MatchForfeitedEvent`, `MatchCancelledEvent`, `MatchPlayerLeftEvent` |                                                                                                                                                                                                          |

#### Derivados (NO consumen `stateVersion`, por destinatario, canal aparte)

| Evento                         | Notas                                                                               |
|--------------------------------|-------------------------------------------------------------------------------------|
| `PlayerHandUpdatedEvent`       | Refresh puntual de mano (p. ej. reconexión, recálculo). Ya NO se usa para repartir. |
| `AvailableActionsUpdatedEvent` | Proyección por jugador del estado actual.                                           |

#### Fuera del stream del match (canales propios, sin cambios)

| Evento                        | Destino                     |
|-------------------------------|-----------------------------|
| `PublicMatchLobbyOpenedEvent` | `/topic/public-match-lobby` |

### Marker

Se introduce un marker `MatchDerivedEvent` (interface vacía) implementado por
`PlayerHandUpdatedEvent` y `AvailableActionsUpdatedEvent`. El resto se considera transicional
por exclusión. La clasificación se valida en tests (lista exhaustiva contra reflection sobre el
paquete `events`).

---

## Notificación de aplicación: `MatchEventNotification`

| Campo          | Tipo                  | Cambia    | Descripción                                                                                                                                                             |
|----------------|-----------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `matchId`      | `MatchId`             | no        |                                                                                                                                                                         |
| `recipients`   | `List<PlayerId>`      | no        | Para transicionales: ambos jugadores. Para info oculta: un destinatario por variante (dos notifications con mismo `stateVersion`). Para derivados: el jugador-objetivo. |
| `eventType`    | `String`              | no        |                                                                                                                                                                         |
| `timestamp`    | `long`                | no        |                                                                                                                                                                         |
| `payload`      | `Map<String, Object>` | no        | Para transicionales con info oculta: ya viene redactado.                                                                                                                |
| `stateVersion` | `Long` (**nullable**) | **nuevo** | Presente sólo en notificaciones transicionales. `null` en derivadas.                                                                                                    |

---

## DTO de transporte WS: `MatchWsEvent`

| Campo          | Tipo                  | Cambia    | Descripción                                                                                                                                         |
|----------------|-----------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `matchId`      | `String`              | no        |                                                                                                                                                     |
| `eventType`    | `String`              | no        |                                                                                                                                                     |
| `timestamp`    | `long`                | no        |                                                                                                                                                     |
| `payload`      | `Map<String, Object>` | no        |                                                                                                                                                     |
| `stateVersion` | `Long` (**nullable**) | **nuevo** | Presente en eventos publicados a `/user/queue/match`. Ausente (Jackson `JsonInclude.NON_NULL`) en eventos publicados a `/user/queue/match-derived`. |

---

## Tabla `matches` — Migración

```sql
-- VXXX__add_match_state_version.sql
ALTER TABLE matches
    ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0;
```

Sin backfill: los matches preexistentes parten de 0 y suman desde la próxima transición. La
monotonía se garantiza dentro del mismo match.

---

## Transiciones de estado

Sin cambios en las transiciones del aggregate per se (las reglas de juego siguen idénticas).
Lo único nuevo es el contador asociado a cada transición.

| Acción                                           | Eventos emitidos                                                                                                                                          | Incrementos de `stateVersion` |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `Match.create` (público)                         | `PublicMatchLobbyOpenedEvent` (no transicional respecto del match)                                                                                        | 0                             |
| `Match.join`                                     | `PlayerJoinedEvent` (+ flujo de `start` si quick/public)                                                                                                  | 1 (+los de `start`)           |
| `start` interno                                  | `GameStartedEvent`, `RoundStartedEvent`, `HandDealtEvent`                                                                                                 | 3                             |
| `playCard`                                       | `CardPlayedEvent`, eventualmente `HandResolvedEvent`, `TurnChangedEvent`/`RoundEndedEvent`/`RoundStartedEvent`/`HandDealtEvent`, `ScoreChangedEvent`, ... | uno por cada evento           |
| `callTruco`                                      | `TrucoCalledEvent`, `TurnChangedEvent`, `AvailableActionsUpdatedEvent` (derivado, NO cuenta)                                                              | 2                             |
| `acceptEnvido`                                   | `EnvidoResolvedEvent`, `ScoreChangedEvent`, etc.                                                                                                          | uno por cada                  |
| `fold`, `abandon`, `forfeit`, `cancel`, `finish` | sus eventos                                                                                                                                               | uno por cada                  |
