# Contrato API: Presencia del usuario

**Feature**: `008-user-presence` | **Fase**: 1 (Design & Contracts)

## `GET /api/me/presence`

Devuelve el estado de ocupación agregado del **usuario autenticado** en los dominios reconectables
(partida, liga, copa, revancha). Operación de **solo lectura** (no modifica estado ni
temporizadores).

### Request

- **Método**: `GET`
- **Ruta**: `/api/me/presence`
- **Autenticación**: requerida. Header `Authorization: Bearer <JWT>`. La identidad del usuario se
  obtiene del `subject` del token (no se acepta id de usuario como parámetro).
- **Parámetros**: ninguno (sin path params, sin query params, sin body).

### Responses

#### `200 OK` — Estado de presencia

`Content-Type: application/json`

Campos `match`, `league`, `cup`, `rematch`: objeto cuando el usuario está ocupado en ese dominio, o
`null` cuando no lo está. `busy` es `true` sii al menos uno de los cuatro es no-nulo.

**Ejemplo A — usuario ocupado en una partida de liga en progreso** (la partida del torneo aparece
tanto en `match` como en `league.currentMatchId`):

```json
{
  "busy": true,
  "match": {
    "id": "8f3a1c20-9e44-4b6a-9b2e-2a1f0c7d4e51",
    "status": "IN_PROGRESS"
  },
  "league": {
    "id": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
    "status": "IN_PROGRESS",
    "currentMatchId": "8f3a1c20-9e44-4b6a-9b2e-2a1f0c7d4e51"
  },
  "cup": null,
  "rematch": null
}
```

**Ejemplo B — usuario con una revancha abierta y nada más:**

```json
{
  "busy": true,
  "match": null,
  "league": null,
  "cup": null,
  "rematch": {
    "id": "5c2e7a90-1d3b-4f8c-8a6e-9b0c1d2e3f40",
    "originMatchId": "8f3a1c20-9e44-4b6a-9b2e-2a1f0c7d4e51"
  }
}
```

**Ejemplo C — usuario totalmente libre** (FR-008 / SC-004):

```json
{
  "busy": false,
  "match": null,
  "league": null,
  "cup": null,
  "rematch": null
}
```

#### `401 Unauthorized` — Token ausente o inválido (FR-002)

`Content-Type: application/json` — usa el `ErrorResponse` estándar del proyecto.

```json
{
  "error": "...",
  "message": "..."
}
```

### Esquemas

#### `UserPresenceResponse`

| Campo     | Tipo               | Nulo | Descripción                              |
|-----------|--------------------|------|------------------------------------------|
| `busy`    | boolean            | No   | `true` sii alguna referencia es no-nula. |
| `match`   | `ActiveMatchRef`   | Sí   | Partida no finalizada del usuario.       |
| `league`  | `ActiveLeagueRef`  | Sí   | Liga del usuario.                        |
| `cup`     | `ActiveCupRef`     | Sí   | Copa del usuario.                        |
| `rematch` | `ActiveRematchRef` | Sí   | Sesión de revancha abierta del usuario.  |

#### `ActiveMatchRef`

| Campo    | Tipo   | Descripción                                        |
|----------|--------|----------------------------------------------------|
| `id`     | string | UUID de la partida.                                |
| `status` | string | `WAITING_FOR_PLAYERS` \| `READY` \| `IN_PROGRESS`. |

#### `ActiveLeagueRef`

| Campo            | Tipo   | Nulo | Descripción                                                    |
|------------------|--------|------|----------------------------------------------------------------|
| `id`             | string | No   | UUID de la liga.                                               |
| `status`         | string | No   | `WAITING_FOR_PLAYERS` \| `WAITING_FOR_START` \| `IN_PROGRESS`. |
| `currentMatchId` | string | Sí   | UUID de la partida actual del torneo (solo si en progreso).    |

#### `ActiveCupRef`

| Campo            | Tipo   | Nulo | Descripción                                                 |
|------------------|--------|------|-------------------------------------------------------------|
| `id`             | string | No   | UUID de la copa.                                            |
| `status`         | string | No   | Estado de la copa (`CupStatus`).                            |
| `currentMatchId` | string | Sí   | UUID de la partida actual del torneo (solo si en progreso). |

#### `ActiveRematchRef`

| Campo           | Tipo   | Descripción                    |
|-----------------|--------|--------------------------------|
| `id`            | string | UUID de la sesión de revancha. |
| `originMatchId` | string | UUID de la partida de origen.  |

### Garantías

- **Solo lectura** (FR-009 / SC-005): invocaciones repetidas no alteran partidas, ligas, copas,
  revanchas ni sus temporizadores de inactividad.
- **Coherencia** (Edge Case del spec): si la partida pertenece a un torneo en progreso,
  `match.id == league.currentMatchId` (o `cup.currentMatchId`).
- **Quick match excluido** (FR-010): la búsqueda de partida rápida nunca aparece en la respuesta.
