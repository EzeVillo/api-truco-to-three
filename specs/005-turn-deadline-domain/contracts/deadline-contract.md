# Contrato expuesto — Deadline de turno

Este feature **no agrega endpoints**. Extiende dos respuestas existentes y agrega dos eventos
WebSocket derivados. También **corrige** una clasificación errónea en `docs/CONTRATOS_API.md`.

## 1. Snapshot REST (campos agregados)

### 1.1 `GET /api/matches/{matchId}` — dentro de `roundGame` (§4.14)

```json
{
  "actionDeadline": 1772768188123,
  "turnDurationMillis": 30000,
  "actionDeadlineSeat": "PLAYER_ONE"
}
```

### 1.2 `GET /api/matches/{matchId}/spectate` — dentro de `currentRound` (§4.15)

Mismos tres campos, idéntica semántica.

| Campo                | Tipo                                   | Semántica                                                                                                            |
|----------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `actionDeadline`     | `epochMillis` absoluto \| `null`       | Instante en que el asiento que debe actuar pierde por timeout. `null` si no corre reloj.                             |
| `turnDurationMillis` | `long`                                 | Duración total del plazo (denominador para anillo/barra de progreso).                                                |
| `actionDeadlineSeat` | `PLAYER_ONE` \| `PLAYER_TWO` \| `null` | Asiento al que aplica el reloj. Puede no coincidir con `currentTurn` ante canto pendiente. `null` si no corre reloj. |

Regla: los tres son `null` simultáneamente cuando el reloj está detenido (INV-3).

## 2. Eventos WebSocket — DERIVADOS (no consumen `stateVersion`)

Viajan por `/user/queue/match` (jugadores) y se reenvían a `/user/queue/match-spectate`
(espectadores), porque son públicos (no privados por asiento). **No** llevan `stateVersion`
(`null`), igual que `AVAILABLE_ACTIONS_UPDATED`.

### 2.1 `ACTION_DEADLINE_SET`

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "ACTION_DEADLINE_SET",
  "timestamp": 1772768158123,
  "payload": {
    "seat": "PLAYER_ONE",
    "actionDeadline": 1772768188123,
    "turnDurationMillis": 30000
  }
}
```

Se emite cada vez que (re)inicia el reloj porque cambió el asiento que debe actuar (cambio de turno,
canto de truco/envido, respuesta, nueva ronda).

### 2.2 `ACTION_DEADLINE_CLEARED`

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "ACTION_DEADLINE_CLEARED",
  "timestamp": 1772768158123,
  "payload": {}
}
```

Se emite cuando el reloj deja de correr (mano resolviéndose, esperas del servidor, o estado sin
asiento que deba actuar).

## 3. Corrección obligatoria a `docs/CONTRATOS_API.md`

**Estado actual (incorrecto tras la decisión D2)**: §9.5 lista `ACTION_DEADLINE_SET` y
`ACTION_DEADLINE_CLEARED` bajo "Eventos transicionales (consumen `stateVersion`)".

**Cambio requerido**: moverlos a una clasificación de **eventos derivados** que **no** avanzan
`stateVersion`, junto a `AVAILABLE_ACTIONS_UPDATED` / `PLAYER_HAND_UPDATED`. Verificar que §4.18,
§4.14, §4.15 y §9.6 queden coherentes (los payloads de §9.6 ya están bien; solo cambia la
clasificación respecto del cursor).

Sin esta corrección, el contrato declara que estos eventos avanzan el cursor de reconciliación,
cuando en realidad llegarán con `stateVersion = null` → el FE detectaría "huecos" falsos.

## 4. Compatibilidad

- Agregar campos nuevos a `roundGame`/`currentRound` es aditivo; clientes que los ignoren no se
  rompen.
- Los dos eventos derivados son nuevos `eventType`; clientes que no los manejen los descartan.
