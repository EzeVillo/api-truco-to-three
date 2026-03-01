# Truco Master – Frontend Handoff (Agent-Ready)

Este documento está pensado para que un agente (o dev FE) implemente el frontend completo sin
inventar contratos.

## 1) Stack recomendado para FE

Recomendación concreta (equilibrio velocidad/calidad para portfolio):

- **React + TypeScript + Vite**
- **TanStack Query** para estado servidor (HTTP)
- **Zustand** para estado UI local (selecciones, toasts, sesión del jugador)
- **React Router** para navegación
- **STOMP.js + SockJS** para tiempo real (o WebSocket nativo con STOMP)
- **Zod** para validación de payloads y mapeo de contratos
- **Vitest + Testing Library** para tests

Por qué este stack:

- Tipado fuerte extremo a extremo.
- Muy buen DX para iterar rápido.
- Separación clara entre estado remoto (Query) y estado local (Zustand).
- STOMP simple de conectar al backend actual.

---

## 2) Prompt maestro para agente FE

Copiar/pegar este prompt en tu agente de código FE:

```text
Quiero que implementes el frontend de Truco Master usando React + TypeScript + Vite.

Objetivo:
- Construir una UI jugable de partidas de truco 1v1 y una UI de torneo liga (todos contra todos por fechas).
- Consumir EXACTAMENTE los contratos HTTP y WebSocket listados en README.
- No inventar endpoints ni campos.

Restricciones:
1) No modificar backend.
2) No hardcodear datos de juego: todo sale de API/WS.
3) Mantener arquitectura FE limpia:
   - src/app (router/providers)
   - src/features/match
   - src/features/tournament
   - src/shared/api
   - src/shared/ws
   - src/shared/ui
   - src/shared/types
4) Modelar tipos TS desde los contratos del README.
5) Manejar errores con el shape ErrorResponse del backend.
6) Implementar tiempo real de match suscribiendo al topic por matchId/playerId.
7) Soportar tanto /ws (nativo) como fallback /ws-sockjs.

Flujo mínimo de producto:
A) Match
- Crear partida.
- Unirse con playerTwoId.
- Ver estado de partida para playerId.
- Ejecutar acciones: play-card, truco, truco/respond, envido, envido/respond, fold.
- Refrescar estado por WS y también por HTTP en eventos clave.

B) Tournament
- Crear torneo con lista de jugadores.
- Ver fixture por fechas (matchdays).
- Mostrar partidos LIBRE sin botón de sincronización.
- Para fixture con matchId real: navegar/abrir match y luego sincronizar resultado con endpoint sync-result.
- Mostrar tabla de posiciones y ganadores.

Criterios de calidad:
- Tipado estricto sin any.
- Hooks por caso de uso (useCreateMatch, useGetMatchState, etc.).
- Componentes presentacionales desacoplados de fetch.
- Estados de loading/error/empty en cada vista.
- Tests de al menos:
  - mapeo de contratos
  - hook de suscripción WS
  - render de matchdays con LIBRE

Entregables:
1) Código FE funcional.
2) README FE con comandos de ejecución.
3) Lista de decisiones técnicas cortas.
```

---

## 3) Contratos Backend (fuente de verdad)

Base URL HTTP: `http://localhost:8080`

### 3.1 Match API

#### `POST /api/matches`

Crea partida.

**Request body:** vacío

**200 OK**

```json
{
  "matchId": "string",
  "playerOneId": "string",
  "playerTwoId": "string"
}
```

---

#### `POST /api/matches/{matchId}/join`

Une jugador 2 a la partida.

**Request**

```json
{
  "playerTwoId": "string"
}
```

**204 No Content**

---

#### `GET /api/matches/{matchId}?playerId={playerId}`

Obtiene estado de partida desde la perspectiva de un jugador.

**200 OK**

```json
{
  "matchId": "string",
  "status": "WAITING_FOR_PLAYERS | IN_PROGRESS | FINISHED",
  "gamesWonPlayerOne": 0,
  "gamesWonPlayerTwo": 0,
  "matchWinner": "string | null",
  "RoundGame": {
    "status": "string",
    "currentTurn": "string",
    "scorePlayerOne": 0,
    "scorePlayerTwo": 0,
    "myCards": [
      { "suit": "ESPADA | BASTO | COPA | ORO", "number": 1 }
    ],
    "roundStatus": "PLAYING | ENVIDO_IN_PROGRESS | TRUCO_IN_PROGRESS | FINISHED",
    "currentTrucoCall": "string | null",
    "winner": "string | null",
    "availableActions": [
      {
        "type": "PLAY_CARD | CALL_TRUCO | CALL_ENVIDO | RESPOND_TRUCO | RESPOND_ENVIDO | FOLD",
        "parameters": ["string"]
      }
    ],
    "playedHands": [
      {
        "cardPlayerOne": { "suit": "ESPADA", "number": 1 },
        "cardPlayerTwo": { "suit": "BASTO", "number": 7 },
        "winner": "string | null"
      }
    ],
    "currentHand": {
      "cardPlayerOne": { "suit": "ESPADA", "number": 1 },
      "cardPlayerTwo": { "suit": "BASTO", "number": 7 },
      "mano": "string"
    }
  }
}
```

Notas:

- `RoundGame` puede ser `null`.
- `number` válido para carta: `1,2,3,4,5,6,7,10,11,12`.

---

#### `POST /api/matches/{matchId}/play-card`

**Request**

```json
{
  "playerId": "string",
  "suit": "ESPADA | BASTO | COPA | ORO",
  "number": 1
}
```

**204 No Content**

---

#### `POST /api/matches/{matchId}/truco`

**Request**

```json
{
  "playerId": "string"
}
```

**204 No Content**

---

#### `POST /api/matches/{matchId}/truco/respond`

**Request**

```json
{
  "playerId": "string",
  "response": "QUIERO | NO_QUIERO | QUIERO_Y_ME_VOY_AL_MAZO"
}
```

**204 No Content**

---

#### `POST /api/matches/{matchId}/envido`

**Request**

```json
{
  "playerId": "string",
  "call": "ENVIDO | REAL_ENVIDO | FALTA_ENVIDO"
}
```

**204 No Content**

---

#### `POST /api/matches/{matchId}/envido/respond`

**Request**

```json
{
  "playerId": "string",
  "response": "QUIERO | NO_QUIERO"
}
```

**200 OK** (si hay resultado de envido)

```json
{
  "pointsMano": 0,
  "pointsPie": 0,
  "winner": "string",
  "pointsWon": 0
}
```

**204 No Content** (si no hay resultado para devolver)

---

#### `POST /api/matches/{matchId}/fold`

**Request**

```json
{
  "playerId": "string"
}
```

**204 No Content**

---

### 3.2 Tournament API

#### `POST /api/tournaments`

Crea torneo liga.

**Request**

```json
{
  "playerIds": ["p1", "p2", "p3", "p4"]
}
```

**200 OK**

```json
{
  "tournamentId": "string",
  "matchdays": [
    {
      "matchdayNumber": 1,
      "fixtures": [
        {
          "fixtureId": "string",
          "matchdayNumber": 1,
          "playerOneId": "string",
          "playerTwoId": "string",
          "matchId": "string | null",
          "winnerPlayerId": "string | null",
          "status": "PENDING | FINISHED | LIBRE"
        }
      ]
    }
  ]
}
```

Notas:

- Si el torneo tiene cantidad impar de jugadores, se generan fixtures `LIBRE`.
- Los fixtures `LIBRE` no tienen `matchId` real.

---

#### `POST /api/tournaments/{tournamentId}/matches/{matchId}/sync-result`

Sincroniza resultado de un match ya jugado contra su fixture de torneo.

**Request body:** vacío

**204 No Content**

---

#### `GET /api/tournaments/{tournamentId}`

Obtiene estado de torneo.

**200 OK**

```json
{
  "tournamentId": "string",
  "status": "IN_PROGRESS | FINISHED",
  "standings": [
    { "playerId": "string", "wins": 0 }
  ],
  "winners": ["string"],
  "matchdays": [
    {
      "matchdayNumber": 1,
      "fixtures": [
        {
          "fixtureId": "string",
          "matchdayNumber": 1,
          "playerOneId": "string",
          "playerTwoId": "string",
          "matchId": "string | null",
          "winnerPlayerId": "string | null",
          "status": "PENDING | FINISHED | LIBRE"
        }
      ]
    }
  ]
}
```

---

### 3.3 WebSocket / STOMP

Endpoints de conexión:

- `ws://localhost:8080/ws` (nativo)
- `http://localhost:8080/ws-sockjs` (SockJS)

Broker topic prefix:

- `/topic`

Suscripción de estado por jugador:

- `/topic/matches/{matchId}/{playerId}`

**Payload publicado por backend:** `MatchStateResponse` (mismo shape que
`GET /api/matches/{matchId}`)

---

### 3.4 Errores

Formato único:

```json
{
  "errorCode": "string",
  "message": "string",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

Status típicos:

- `404 Not Found` (ApplicationException NOT_FOUND)
- `422 Unprocessable Content` (DomainException o ApplicationException UNPROCESSABLE)
- `500 Internal Server Error` (unexpected)

---

## 4) Reglas FE importantes (para no romper flujo)

- Mostrar acciones **solo** desde `availableActions`.
- `myCards` representa cartas visibles del jugador consultado/suscripto.
- En torneo, no permitir acción sobre fixtures con `status = LIBRE`.
- `sync-result` se ejecuta solo cuando el `matchId` asociado ya terminó.
- Considerar `200` y `204` en `envido/respond`.

---

## 5) Checklist de implementación FE

- [ ] Tipos TS de todos los contratos anteriores.
- [ ] Cliente HTTP con manejo uniforme de `ErrorResponse`.
- [ ] Cliente STOMP con reconexión básica.
- [ ] Pantalla Match (crear, join, jugar, responder cantos, fold, estado vivo).
- [ ] Pantalla Tournament (crear, fixture por fechas, standings, winners, sync-result).
- [ ] Tests de contratos y render de fixtures `LIBRE`.

---

## 6) Comandos sugeridos FE

```bash
npm create vite@latest truco-fe -- --template react-ts
cd truco-fe
npm install @tanstack/react-query zustand react-router-dom zod @stomp/stompjs sockjs-client
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
npm run dev
```
