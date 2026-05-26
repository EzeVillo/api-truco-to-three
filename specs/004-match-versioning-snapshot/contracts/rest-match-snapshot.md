# Contrato — Snapshot REST del match

Endpoints afectados (cambio **aditivo**, no rompe clientes existentes):

- `GET /api/matches/{matchId}` → `MatchStateResponse`
- `GET /api/matches/{matchId}/spectate` → `SpectatorMatchStateResponse`

## Cambio

Se agrega el campo `stateVersion: integer (int64)` en la raíz del response.

### `MatchStateResponse` (diff)

```diff
 {
   "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
   "status": "IN_PROGRESS",
   "viewerSeat": "PLAYER_ONE",
   "playerOneUsername": "juancho",
   "playerTwoUsername": "martina",
   "gamesToPlay": 3,
   "scorePlayerOne": 2,
   "scorePlayerTwo": 1,
   "gamesWonPlayerOne": 1,
   "gamesWonPlayerTwo": 0,
   "matchWinner": null,
+  "stateVersion": 42,
   "roundGame": { ... }
 }
```

### `SpectatorMatchStateResponse` (diff)

Mismo agregado: campo `stateVersion: integer (int64)` en la raíz.

## Semántica

- `stateVersion` es el número de la **última transición aplicada** al match al momento de
  generar el snapshot. Si todavía no ocurrió ninguna transición (p.ej. match creado, sin join
  ni start), vale `0`.
- Es el cursor que el cliente debe usar para reconciliar contra el stream WS: cualquier evento
  recibido con `stateVersion <= snapshot.stateVersion` ya está reflejado en el snapshot y debe
  descartarse.

## Compatibilidad

- Cliente viejo: ignora el campo y sigue funcionando.
- Cliente nuevo: lee el campo y lo usa como punto de partida para `nextExpectedStateVersion =
  snapshot.stateVersion + 1`.

## OpenAPI / Swagger

Agregar la anotación `@Schema(description = "Número de la última transición aplicada al match;
sirve como cursor para reconciliar con el stream WebSocket", example = "42")` al campo
`stateVersion` en ambos response records.
