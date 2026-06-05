# Data Model: Estado de presencia / ocupación del usuario

**Feature**: `008-user-presence` | **Fase**: 1 (Design & Contracts)

No hay entidades persistentes nuevas: **no se crean tablas ni migraciones**. El modelo describe los
DTO de lectura (capa `application`) que agregan datos existentes y su mapeo a la respuesta HTTP.

---

## Vista de lectura: `UserPresenceDTO` (application/dto)

Representación agregada del estado de ocupación del usuario autenticado. Todos los campos de
referencia son **opcionales** (`null` cuando el usuario no está ocupado en ese dominio).

| Campo     | Tipo                  | Nulo | Descripción                                                   |
|-----------|-----------------------|------|---------------------------------------------------------------|
| `busy`    | `boolean`             | No   | `true` si al menos una referencia es no-nula (FR-007).        |
| `match`   | `ActiveMatchRefDTO`   | Sí   | Partida no finalizada del usuario, o `null` (FR-003, FR-008). |
| `league`  | `ActiveLeagueRefDTO`  | Sí   | Liga en espera/en progreso del usuario, o `null` (FR-004).    |
| `cup`     | `ActiveCupRefDTO`     | Sí   | Copa en espera/en progreso del usuario, o `null` (FR-005).    |
| `rematch` | `ActiveRematchRefDTO` | Sí   | Sesión de revancha abierta del usuario, o `null` (FR-006).    |

**Regla de derivación**: `busy = match != null || league != null || cup != null || rematch != null`.
Se calcula en una factory estática del DTO (p. ej.
`UserPresenceDTO.of(match, league, cup, rematch)`)
para garantizar la invariante FR-007 en un único lugar.

---

## Sub-referencias

### `ActiveMatchRefDTO`

| Campo    | Tipo     | Origen                                   |
|----------|----------|------------------------------------------|
| `id`     | `String` | `Match.getId().value().toString()`       |
| `status` | `String` | `Match.getStatus()` (enum `MatchStatus`) |

Valores posibles de `status`: `WAITING_FOR_PLAYERS`, `READY`, `IN_PROGRESS` (los terminales
`FINISHED`/`CANCELLED` nunca aparecen: se excluyen por la consulta).

### `ActiveLeagueRefDTO`

| Campo            | Tipo     | Nulo | Origen                                                                 |
|------------------|----------|------|------------------------------------------------------------------------|
| `id`             | `String` | No   | `League.getId().value().toString()`                                    |
| `status`         | `String` | No   | `League.getStatus()` (enum `LeagueStatus`)                             |
| `currentMatchId` | `String` | Sí   | Id de la partida actual del torneo cuando `IN_PROGRESS`; si no, `null` |

Valores posibles de `status`: `WAITING_FOR_PLAYERS`, `WAITING_FOR_START`, `IN_PROGRESS`.

### `ActiveCupRefDTO`

| Campo            | Tipo     | Nulo | Origen                                                               |
|------------------|----------|------|----------------------------------------------------------------------|
| `id`             | `String` | No   | `Cup.getId().value().toString()`                                     |
| `status`         | `String` | No   | `Cup.getStatus()` (enum `CupStatus`)                                 |
| `currentMatchId` | `String` | Sí   | Id de la partida actual del torneo cuando en progreso; si no, `null` |

### `ActiveRematchRefDTO`

| Campo           | Tipo     | Origen                                                 |
|-----------------|----------|--------------------------------------------------------|
| `id`            | `String` | `RematchSession.getId().value().toString()`            |
| `originMatchId` | `String` | `RematchSession.getOriginMatchId().value().toString()` |

---

## Reglas de validación

- La query `GetUserPresenceQuery(PlayerId requester)` exige `requester` no nulo
  (`Objects.requireNonNull`), igual que las queries existentes.
- No hay validación de negocio adicional: todas las combinaciones de referencias presentes/ausentes
  son válidas (incluido el caso "todas nulas" → `busy = false`, FR-008/SC-004).

## Coherencia entre referencias (Edge Case del spec)

Cuando el usuario está en una partida que pertenece a un torneo en progreso:

- `match.id == league.currentMatchId` (o `cup.currentMatchId`).
- Ambas referencias (torneo y partida) están presentes simultáneamente.

Esto se garantiza derivando `currentMatchId` de la **misma** partida no finalizada usada para
`match`, apoyándose en la invariante de unicidad (un jugador tiene a lo sumo una partida no
finalizada). Ver Decisión 4 de [research.md](research.md).

## Puerto de salida modificado

`MatchQueryRepository` (domain) agrega:

```java
Optional<Match> findUnfinishedByPlayer(PlayerId playerId);
```

Semántica idéntica a `hasUnfinishedMatch` pero devolviendo el agregado: partidas con
`status NOT IN ('FINISHED','CANCELLED')` donde el jugador es `playerOne` o `playerTwo`, ordenadas
por `lastActivityAt DESC` para determinismo (a lo sumo un resultado por la invariante).
