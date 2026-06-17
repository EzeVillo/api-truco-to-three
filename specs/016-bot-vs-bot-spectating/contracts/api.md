# Contrato (deltas): Partidas Bot vs Bot Espectables

Cambios al contrato REST + WebSocket. Referencia autoritativa a sincronizar en
`docs/CONTRATOS_API.md` al implementar.

## 1. REST — Crear partida bot vs bot (NUEVO)

`POST /api/matches/bot-vs-bot`

Auth: Bearer requerido.

Request body:

```json
{
  "botOneId": "11111111-1111-1111-1111-111111111111",
  "botTwoId": "22222222-2222-2222-2222-222222222222",
  "gamesToPlay": 3
}
```

- `gamesToPlay` ∈ `{1, 3, 5}` (formato de serie; nunca `2`).
- `botOneId` y `botTwoId` deben ser bots existentes en el catálogo y **distintos** entre sí.

Response `200`:

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7"
}
```

Errores:

| Código      | Causa                                                                      |
|-------------|----------------------------------------------------------------------------|
| `400`       | Body inválido/faltante; `gamesToPlay` fuera de `{1,3,5}`; bots iguales.    |
| `401`       | Token ausente o inválido.                                                  |
| `404`       | Alguno de los bots no existe en el catálogo.                               |
| `409`/`422` | El usuario ya está ocupado (incluye ser dueño de otra bot-match en curso). |

Notas:

- La partida arranca `IN_PROGRESS` y los bots juegan solos hasta una conclusión.
- La partida es `PRIVATE`: **no** aparece en el lobby público.
- Crear esta partida deja al solicitante **ocupado por autoría** hasta que termine (busy total).
- No genera chat ni sesión de revancha.

## 2. REST — Estado de espectador (MODIFICADO)

`GET /api/matches/{matchId}/spectate`

Para partidas bot-vs-bot, `currentRound` incluye **las manos completas de ambos bots**
(`handPlayerOne`, `handPlayerTwo`). Para partidas con humanos, ambos campos son `null` (sin cambios
respecto del contrato actual).

```jsonc
{
  "matchId": "…",
  "status": "IN_PROGRESS",
  "playerOneUsername": "Bot Alfa",
  "playerTwoUsername": "Bot Beta",
  "...": "…",
  "currentRound": {
    "...": "…",
    "playedHands": [ /* … */ ],
    "currentHand": { "cardPlayerOne": null, "cardPlayerTwo": null, "mano": "Bot Alfa" },
    "handPlayerOne": [ { "suit": "ESPADA", "number": 1 }, { "suit": "ORO", "number": 7 } ],
    "handPlayerTwo": [ { "suit": "COPA", "number": 3 }, { "suit": "BASTO", "number": 12 } ]
  },
  "spectatorCount": 1
}
```

Reglas:

- **Solo el creador** del match puede espectarlo. Un no-creador recibe `422`.
- `handPlayerOne`/`handPlayerTwo` = cartas **en mano restantes** de cada bot (lo que aún no jugó).
- Es el estado **inicial** (para conectarse a media partida). Las actualizaciones de mano llegan por
  WebSocket (ver §4), no por re-consulta REST.

## 3. REST — Presencia (MODIFICADO)

`GET /api/me/presence`

Se agrega el campo `ownedBotMatch` (nullable). Está presente cuando el usuario es dueño de una
partida bot-vs-bot en curso; en ese caso `busy = true`.

```jsonc
{
  "busy": true,
  "match": null,
  "league": null,
  "cup": null,
  "rematch": null,
  "quickMatch": null,
  "spectating": null,
  "ownedBotMatch": {
    "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
    "status": "IN_PROGRESS"
  }
}
```

- `ownedBotMatch` marca ocupación por **autoría**, independiente de `spectating` (mirar es
  opcional).
- Si el usuario además está espectando esa misma partida, `spectating` y `ownedBotMatch` pueden
  apuntar al mismo `matchId` (el FE puede deduplicar).

## 4. WebSocket — Spectate (deltas)

Cola `/user/queue/match-spectate` (sin cambios de destino).

- `SPECTATE_STATE` (`{ matchState }`): para bot-vs-bot, `matchState` incluye `handPlayerOne` y
  `handPlayerTwo` dentro de `currentRound` (misma forma que `GET /spectate`). Estado inicial.
- `SPECTATE_ERROR` (`{ error }`): se devuelve si un usuario que **no es el creador** intenta
  suscribirse a una partida bot-vs-bot.
- Eventos públicos en vivo (`CARD_PLAYED`, `TRUCO_*`, `ENVIDO_*`, `SCORE_CHANGED`, `ROUND_STARTED`,
  `GAME_*`, `MATCH_FINISHED`, `ACTION_DEADLINE_*`, etc.) se siguen reenviando.

**Manos por WebSocket (solo bot-vs-bot):** el espectador (creador) recibe, como un jugador normal,
los eventos de mano de **ambos** asientos:

- `HAND_DEALT` → `{ player_one: [cartas], player_two: [cartas] }` en cada reparto.
- `PLAYER_HAND_UPDATED` → `{ seat, cards }` cuando una mano cambia (tras jugar una carta), para
  ambos asientos.

**Cambio de comportamiento (cierre de fuga):** en partidas **con humanos**, los espectadores
**dejan de recibir** `HAND_DEALT` (hoy se reenviaba con ambas manos, una fuga). Siguen sin recibir
`PLAYER_HAND_UPDATED` ni `AVAILABLE_ACTIONS_UPDATED`. Es decir, fuera de bot-vs-bot ningún
espectador
ve cartas en mano. `AVAILABLE_ACTIONS_UPDATED` tampoco se reenvía en bot-vs-bot.

## 5. Enums / reglas afectadas

- `POST /api/matches/bot-vs-bot`: `gamesToPlay` acepta exactamente `{1, 3, 5}`.
- Nuevo motivo de ocupación de presencia/disponibilidad: ser dueño de una bot-match en curso.
- Sin nuevos `eventType` WebSocket (se reutilizan los existentes de spectate).
