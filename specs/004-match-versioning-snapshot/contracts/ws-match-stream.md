# Contrato — Stream WebSocket del match

## Destinos STOMP

| Destino                      | Antes                                                                                             | Después                                                                                                                                                                             |
|------------------------------|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/user/queue/match`          | Recibe todos los eventos de match (incluidos `PLAYER_HAND_UPDATED`, `AVAILABLE_ACTIONS_UPDATED`). | Recibe **sólo eventos transicionales**, cada uno con campo `stateVersion`.                                                                                                          |
| `/user/queue/match-derived`  | — (nuevo)                                                                                         | Recibe notificaciones derivadas por jugador (no cuentan para la secuencia): `PLAYER_HAND_UPDATED`, `AVAILABLE_ACTIONS_UPDATED`. Mismo shape de evento, pero **sin** `stateVersion`. |
| `/user/queue/match-spectate` | Espectadores.                                                                                     | Sin cambios funcionales, pero ahora los eventos transicionales que llegan incluyen `stateVersion`.                                                                                  |

## Shape del evento transicional (`/user/queue/match`)

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "CARD_PLAYED",
  "timestamp": 1772768158123,
  "stateVersion": 17,
  "payload": {
    "seat": "PLAYER_ONE",
    "card": {
      "suit": "ESPADA",
      "number": 1
    }
  }
}
```

## Shape del evento derivado (`/user/queue/match-derived`)

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "AVAILABLE_ACTIONS_UPDATED",
  "timestamp": 1772768158129,
  "payload": {
    "seat": "PLAYER_ONE",
    "availableActions": [
      {
        "type": "PLAY_CARD"
      },
      {
        "type": "CALL_TRUCO"
      }
    ]
  }
}
```

> El campo `stateVersion` está **ausente** (serializado con `JsonInclude.NON_NULL`).

## Evento nuevo: `HAND_DEALT`

Reemplaza el rol de reparto que cumplían los dos `PLAYER_HAND_UPDATED` históricos.

- `eventType`: `"HAND_DEALT"`
- Canal: `/user/queue/match` (es transicional)
- Mismo `stateVersion` y mismo `eventType` para todos los jugadores; el payload se redacta por
  destinatario.

Shape recibido por cada jugador:

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "HAND_DEALT",
  "timestamp": 1772768158100,
  "stateVersion": 5,
  "payload": {
    "seat": "PLAYER_ONE",
    "cards": [
      {
        "suit": "ESPADA",
        "number": 1
      },
      {
        "suit": "BASTO",
        "number": 7
      },
      {
        "suit": "ORO",
        "number": 4
      }
    ]
  }
}
```

El payload contiene **únicamente** la mano del destinatario. No incluye campos `opponentCards`
(ni `null`), ni cantidades, ni hashes, ni metadatos derivables. (FR-005, FR-011)

Espectadores que reciban este evento por `/user/queue/match-spectate` reciben:

```json
{
  "matchId": "...",
  "eventType": "HAND_DEALT",
  "stateVersion": 5,
  "payload": {}
}
```

(sin campo `cards`, ni `seat`).

## Reglas de reconciliación cliente (referencia)

Pseudocódigo:

```text
init:
  appliedStateVersion ← snapshot.stateVersion
  pendingBuffer ← []                          // eventos recibidos por WS antes del snapshot

on receive transitionalEvent e:
  if appliedStateVersion is null:
    pendingBuffer.push(e)                     // todavía no llegó snapshot
    return
  if e.stateVersion <= appliedStateVersion:
    discard                                   // duplicado o ya en snapshot
  else if e.stateVersion == appliedStateVersion + 1:
    apply(e); appliedStateVersion += 1
    flush pendingBuffer in order
  else:
    // hueco: e.stateVersion > appliedStateVersion + 1
    request snapshot again; reset state

on receive derivedEvent e:
  apply(e) sin tocar appliedStateVersion
```

## Compatibilidad

- Aditivo en `/user/queue/match`: clientes que no lean `stateVersion` siguen viendo los mismos
  `eventType` y `payload` para los eventos que aún se entregan por ahí.
- **Breaking** para clientes que esperan `PLAYER_HAND_UPDATED` / `AVAILABLE_ACTIONS_UPDATED`
  en `/user/queue/match`: deben suscribirse también a `/user/queue/match-derived`. Documentar
  en `docs/CONTRATOS_API.md` y coordinar con frontend.
