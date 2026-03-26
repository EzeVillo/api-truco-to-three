# Truco API - Contratos para Frontend

Este documento resume todos los contratos que expone el backend para el equipo de FE:

- REST HTTP (request/response)
- Autenticacion (JWT)
- WebSocket/STOMP (eventos en tiempo real)
- Estados, enums y formato de errores

Base URL (local):

- `http://localhost:8080`

## 1. Autenticacion y reglas generales

### 1.1 Token

- Tipo: `Bearer <JWT>`
- Se obtiene en:
    - `POST /api/auth/register` (registro de usuario)
    - `POST /api/auth/login` (login con credenciales)
    - `POST /api/auth/guest` (acceso como invitado)
- Claims relevantes:
    - `sub`: `playerId` (UUID)
    - `iss`, `aud`, `iat`, `exp`

### 1.2 Endpoints protegidos

Segun configuracion de seguridad:

- Publicos:
    - `POST /api/auth/register`
    - `POST /api/auth/login`
    - `POST /api/auth/guest`
- Requieren Bearer token:
    - Todo `/api/**` no listado arriba (matches, leagues, etc.)

### 1.3 Regla de pertenencia de token

En endpoints protegidos de match, el token debe tener el mismo `matchId` del path.
Si no coincide, responde `401`.

### 1.4 IDs

Aunque algunos ejemplos de anotaciones muestran `match-123`, en runtime se parsea UUID.
Enviar siempre UUID valido en:

- `matchId`
- `leagueId`
- `playerId` (cuando aplique en payload/respuesta)

## 2. Contrato de errores

Formato estandar para errores (`ErrorResponse`):

```json
{
  "errorCode": "com.villo.truco.application.exceptions.UnauthorizedAccessException",
  "message": "Missing authentication token",
  "timestamp": "2026-03-06T03:15:30Z"
}
```

HTTP status usados:

- `401` Unauthorized
- `404` Not Found
- `405` Method Not Allowed
- `422` Unprocessable Content
- `500` Internal Server Error

## 3. API REST - Auth

### 3.1 Registrar usuario

`POST /api/auth/register`

Request:

```json
{
  "username": "juancho",
  "password": "secret123"
}
```

Response `200`:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "accessToken": "<jwt>"
}
```

Errores:

- `422` si el username ya esta en uso

### 3.2 Login

`POST /api/auth/login`

Request:

```json
{
  "username": "juancho",
  "password": "secret123"
}
```

Response `200`:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "accessToken": "<jwt>"
}
```

Errores:

- `401` si las credenciales son invalidas (username no existe o contraseña incorrecta)

### 3.3 Acceso como invitado

`POST /api/auth/guest`

Request: sin body.

Response `200`:

```json
{
  "playerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "accessToken": "<jwt>"
}
```

No persiste cuenta. El `playerId` es efimero.

## 4. API REST - Matches

### 4.1 Crear partida

`POST /api/matches`

Request:

```json
{
  "gamesToPlay": 3
}
```

Reglas para `gamesToPlay`:

- Valores permitidos: `1`, `3`, `5`
- `gamesToWin` interno se calcula como `gamesToPlay / 2 + 1`

Response `200`:

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "accessToken": "<jwt>",
  "inviteCode": "ABC123"
}
```

### 4.2 Unirse a partida

`POST /api/matches/join`

Request:

```json
{
  "inviteCode": "ABC123"
}
```

Response `200`:

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "accessToken": "<jwt>"
}
```

### 4.3 Iniciar partida

`POST /api/matches/{matchId}/start`

Auth: Bearer requerido.

Response `204` sin body.

### 4.4 Jugar carta

`POST /api/matches/{matchId}/play-card`

Auth: Bearer requerido.

Request:

```json
{
  "suit": "ESPADA",
  "number": 1
}
```

Response `204` sin body.

### 4.5 Cantar truco

`POST /api/matches/{matchId}/truco`

Auth: Bearer requerido.

Request body: sin body.

Response `204` sin body.

### 4.6 Responder truco

`POST /api/matches/{matchId}/truco/respond`

Auth: Bearer requerido.

Request:

```json
{
  "response": "QUIERO"
}
```

Response `204` sin body.

### 4.7 Cantar envido

`POST /api/matches/{matchId}/envido`

Auth: Bearer requerido.

Request:

```json
{
  "call": "ENVIDO"
}
```

Response `204` sin body.

### 4.8 Responder envido

`POST /api/matches/{matchId}/envido/respond`

Auth: Bearer requerido.

Request:

```json
{
  "response": "NO_QUIERO"
}
```

Response `204` sin body.

### 4.9 Irse al mazo

`POST /api/matches/{matchId}/fold`

Auth: Bearer requerido.

Request body: sin body.

Response `204` sin body.

### 4.10 Abandonar partida

`POST /api/matches/{matchId}/abandon`

Auth: Bearer requerido.

Request body: sin body.

Response `204` sin body.

El jugador autenticado abandona voluntariamente la partida. El oponente gana automáticamente.

Errores:

- `422` si el jugador no pertenece a la partida o la partida está en estado `WAITING_FOR_PLAYERS` o
  ya `FINISHED`

Nota: el abandono voluntario sincroniza automáticamente el resultado al liga si la partida
pertenece
a uno (igual que el timeout automático y el fin normal de partida).

### 4.11 Obtener estado de partida

`GET /api/matches/{matchId}`

Auth: Bearer requerido.

Response `200`:

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "status": "IN_PROGRESS",
  "gamesWonPlayerOne": 1,
  "gamesWonPlayerTwo": 0,
  "matchWinner": null,
  "roundGame": {
    "status": "IN_PROGRESS",
    "currentTurn": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "scorePlayerOne": 2,
    "scorePlayerTwo": 1,
    "myCards": [
      {
        "suit": "ESPADA",
        "number": 1
      },
      {
        "suit": "BASTO",
        "number": 7
      }
    ],
    "roundStatus": "PLAYING",
    "currentTrucoCall": null,
    "winner": null,
    "availableActions": [
      {
        "type": "PLAY_CARD"
      },
      {
        "type": "CALL_TRUCO"
      }
    ],
    "playedHands": [
      {
        "cardPlayerOne": {
          "suit": "ORO",
          "number": 3
        },
        "cardPlayerTwo": {
          "suit": "COPA",
          "number": 5
        },
        "winner": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
      }
    ],
    "currentHand": {
      "cardPlayerOne": null,
      "cardPlayerTwo": null,
      "mano": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
    }
  }
}
```

- `roundGame` es `null` si la partida no está `IN_PROGRESS`
- `myCards` contiene solo las cartas del jugador autenticado
- `availableActions` refleja las acciones disponibles para el jugador autenticado

Errores:

- `404` si la partida no existe
- `422` si el jugador no pertenece a la partida

## 5. API REST - Leagues

### 5.1 Crear liga

`POST /api/leagues`

Request:

```json
{
  "numberOfPlayers": 4,
  "gamesToPlay": 3
}
```

Reglas para `numberOfPlayers`:

- Valores permitidos: entre `3` y `8`
- El `playerId` del creador se genera internamente (igual que en matches)

Reglas para `gamesToPlay`:

- Valores permitidos: `1`, `3`, `5`
- Define la cantidad de partidas por fixture del liga
- `gamesToWin` interno se calcula como `gamesToPlay / 2 + 1`

Response `200`:

```json
{
  "leagueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "inviteCode": "ABCD1234",
  "accessToken": "<jwt>"
}
```

### 5.2 Unirse a liga

`POST /api/leagues/join`

Request:

```json
{
  "inviteCode": "ABCD1234"
}
```

Response `200`:

```json
{
  "leagueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 5.3 Salir de liga

`POST /api/leagues/{leagueId}/leave`

Auth: Bearer requerido (token del liga).

Response `204` sin body.

### 5.4 Iniciar liga

`POST /api/leagues/{leagueId}/start`

Auth: Bearer requerido (solo el creador).

Response `204` sin body.

### 5.5 Obtener estado de liga

`GET /api/leagues/{leagueId}`

Response `200`: estado completo, tabla y fixtures del liga.

## 6. API REST - Copas

Copa por eliminación directa (single elimination bracket).

### 6.1 Crear copa

`POST /api/cups`

Auth: Bearer requerido.

Request:

```json
{
  "numberOfPlayers": 4,
  "gamesToPlay": 3
}
```

Reglas para `numberOfPlayers`:

- Valores permitidos: entre `4` y `8`
- El creador se une automáticamente

Reglas para `gamesToPlay`:

- Valores permitidos: `1`, `3`, `5`
- Define la cantidad de partidas por cruce (bout)

Response `200`:

```json
{
  "cupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "inviteCode": "ABCD1234"
}
```

### 6.2 Unirse a copa

`POST /api/cups/join`

Auth: Bearer requerido.

Request:

```json
{
  "inviteCode": "ABCD1234"
}
```

Response `200`:

```json
{
  "cupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Errores:

- `404` si no existe una copa con ese código de invitación
- `422` si la copa no está en estado `WAITING_FOR_PLAYERS`, ya está llena o el jugador ya está en
  ella

### 6.3 Salir de copa

`POST /api/cups/{cupId}/leave`

Auth: Bearer requerido.

Response `204` sin body.

Errores:

- `422` si la copa ya inició, o si el jugador es el creador (el creador no puede salir)

### 6.4 Iniciar copa

`POST /api/cups/{cupId}/start`

Auth: Bearer requerido (solo el creador).

Response `204` sin body.

Al iniciar:

- Se genera el bracket aleatoriamente
- Se crean automáticamente los matches de la primera ronda (bouts `PENDING`)
- Si hay byes (n de jugadores no es potencia de 2), los jugadores con bye avanzan solos a la
  siguiente ronda

Errores:

- `422` si no es el creador, o la copa no tiene todos los jugadores (`WAITING_FOR_PLAYERS`)

### 6.5 Obtener estado de copa

`GET /api/cups/{cupId}`

Auth: Bearer requerido (solo participantes).

Response `200`:

```json
{
  "cupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "IN_PROGRESS",
  "rounds": [
    {
      "roundNumber": 1,
      "roundName": "Semifinal",
      "bouts": [
        {
          "boutId": "b1c2d3e4-...",
          "roundNumber": 1,
          "bracketPosition": 0,
          "playerOneId": "uuid-p1",
          "playerTwoId": "uuid-p2",
          "matchId": "uuid-match",
          "winnerId": null,
          "status": "PENDING"
        }
      ]
    },
    {
      "roundNumber": 2,
      "roundName": "Final",
      "bouts": [
        {
          "boutId": "c2d3e4f5-...",
          "roundNumber": 2,
          "bracketPosition": 0,
          "playerOneId": null,
          "playerTwoId": null,
          "matchId": null,
          "winnerId": null,
          "status": "AWAITING"
        }
      ]
    }
  ],
  "champion": null
}
```

Cuando la copa finaliza, `status` es `FINISHED` y `champion` contiene el `playerId` del campeón.

Errores:

- `404` si la copa no existe
- `422` si el jugador no pertenece a la copa

### 6.6 Estados de la copa (`status`)

| Estado                | Descripción                                                     |
|-----------------------|-----------------------------------------------------------------|
| `WAITING_FOR_PLAYERS` | Creada, esperando que se unan los jugadores                     |
| `WAITING_FOR_START`   | Todos los jugadores se unieron, esperando que el creador inicie |
| `IN_PROGRESS`         | Bracket generado y partidas en curso                            |
| `FINISHED`            | Copa finalizada, hay campeón                                    |

### 6.7 Estados de un bout (`BoutStatus`)

| Estado     | Descripción                                                          |
|------------|----------------------------------------------------------------------|
| `AWAITING` | Ronda futura, aún sin jugadores asignados                            |
| `PENDING`  | Ambos jugadores asignados, match creado y en curso                   |
| `FINISHED` | Match terminado, hay ganador                                         |
| `BYE`      | Jugador sin rival (bye), avanza automáticamente a la siguiente ronda |

### 6.8 Nombres de ronda (`roundName`)

El campo `roundName` es informativo según la distancia a la final:

| Rondas desde la final | `roundName`        |
|-----------------------|--------------------|
| 0                     | `Final`            |
| 1                     | `Semifinal`        |
| 2                     | `Cuartos de final` |
| 3+                    | `Ronda N`          |

### 6.9 Flujo de avance automático

El bracket avanza automáticamente via eventos internos:

1. Cuando un match de copa termina → el ganador avanza al siguiente bout
2. Cuando un jugador abandona un match de copa → se registra como forfeit; el rival avanza
   automáticamente
3. Si el rival ya había forfeiteado → el bout se resuelve sin crear match (cascade forfeit)
4. Al llegar a la final y resolverse → la copa pasa a `FINISHED` con el `champion`

El FE no necesita llamar ningún endpoint adicional para el avance: solo suscribirse a los eventos
WebSocket del match activo y consultar `GET /api/cups/{cupId}` para ver el estado actualizado del
bracket.

## 7. API REST - Chat

Chat en tiempo real asociado a un match, liga o copa. Se crea automáticamente al iniciar el recurso
padre y se elimina al finalizar o cancelarse.

- **Match**: chat creado en `GameStartedEvent` (primer game), eliminado en `MatchFinishedEvent` o
  `MatchForfeitedEvent`
- **Liga**: chat creado en `LeagueStartedEvent`, eliminado en `LeagueFinishedEvent` o
  `LeagueCancelledEvent`
- **Copa**: chat creado en `CupStartedEvent`, eliminado en `CupFinishedEvent` o
  `CupCancelledEvent`

Reglas de negocio:

- Máximo **50 mensajes** por chat (buffer circular: al llegar al límite se descarta el más antiguo)
- Máximo **500 caracteres** por mensaje
- **Rate limit**: 2 segundos mínimo entre mensajes del mismo jugador
- Solo **participantes** del recurso padre pueden enviar y leer mensajes

### 7.1 Enviar mensaje

`POST /api/chats/{chatId}/messages`

Auth: Bearer requerido.

Request:

```json
{
  "content": "Buena mano!"
}
```

Response `204` sin body.

Errores:

- `404` si el chat no existe
- `422` si el jugador no pertenece al chat, el mensaje está vacío, excede 500 caracteres, o
  viola el rate limit (2 segundos)

### 7.2 Obtener mensajes por chatId

`GET /api/chats/{chatId}/messages`

Auth: Bearer requerido.

Response `200`:

```json
{
  "chatId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "parentType": "MATCH",
  "parentId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "messages": [
    {
      "messageId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
      "senderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "content": "Buena mano!",
      "sentAt": 1772768158123
    }
  ]
}
```

Errores:

- `404` si el chat no existe
- `422` si el jugador no pertenece al chat

### 7.3 Buscar chat por recurso padre

`GET /api/chats/by-parent/{parentType}/{parentId}`

Auth: Bearer requerido.

Path params:

- `parentType`: `MATCH`, `LEAGUE` o `CUP`
- `parentId`: UUID del match, liga o copa

Response `200`: misma estructura que 7.2.

Errores:

- `404` si no existe chat para ese recurso
- `422` si el jugador no pertenece al chat

## 8. Enums y valores permitidos

Estos valores se parsean con `Enum.valueOf(...)`, por lo que deben enviarse exactamente igual (
mayusculas y guiones bajos).

### 8.1 Requests

- `PlayCardRequest.suit`:
    - `ESPADA`, `BASTO`, `COPA`, `ORO`
- `CallEnvidoRequest.call`:
    - `ENVIDO`, `REAL_ENVIDO`, `FALTA_ENVIDO`
- `RespondEnvidoRequest.response`:
    - `QUIERO`, `NO_QUIERO`
- `RespondTrucoRequest.response`:
    - `QUIERO`, `NO_QUIERO`, `QUIERO_Y_ME_VOY_AL_MAZO`

### 8.2 Estados en respuestas

- `MatchStateResponse.status`:
    - `WAITING_FOR_PLAYERS`, `IN_PROGRESS`, `FINISHED`
- `RoundStateResponse.roundStatus`:
    - `PLAYING`, `ENVIDO_IN_PROGRESS`, `TRUCO_IN_PROGRESS`, `FINISHED`
- `RoundStateResponse.currentTrucoCall`:
    - `TRUCO`, `RETRUCO`, `VALE_CUATRO` (o `null`)
- `AvailableActionResponse.type`:
    - `PLAY_CARD`, `CALL_TRUCO`, `CALL_ENVIDO`, `RESPOND_TRUCO`, `RESPOND_ENVIDO`, `FOLD`

## 9. WebSocket / STOMP

### 9.1 Endpoints de conexion

- WebSocket nativo: `/ws`
- SockJS: `/ws-sockjs`

Broker/prefijos:

- `setApplicationDestinationPrefixes`: `/app`
- `setUserDestinationPrefix`: `/user`
- Broker habilitado en: `/topic`, `/queue`

Nota: no hay `@MessageMapping` para mensajes cliente->server.

### 9.2 Autenticacion WS

En frame STOMP `CONNECT` enviar header:

- `Authorization: Bearer <jwt>`

El token debe contener `sub` (playerId).

### 9.3 Suscripciones permitidas

Suscripciones permitidas por interceptor:

- `/user/queue/match` — eventos de match
- `/user/queue/league` — eventos de liga
- `/user/queue/cup` — eventos de copa
- `/user/queue/chat` — eventos de chat en tiempo real

Cualquier otro destino se rechaza

### 9.4 Forma del evento WS

Cada tipo de recurso tiene su propia estructura de evento:

**Match** (`/user/queue/match`):

```json
{
  "matchId": "8b9c5936-9a1f-45ec-a587-24306689f6f7",
  "eventType": "CARD_PLAYED",
  "timestamp": 1772768158123,
  "payload": {
    "seat": "PLAYER_ONE",
    "card": {
      "suit": "ESPADA",
      "number": 1
    }
  }
}
```

**Liga** (`/user/queue/league`):

```json
{
  "leagueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "eventType": "LEAGUE_STARTED",
  "timestamp": 1772768158123,
  "payload": {}
}
```

**Copa** (`/user/queue/cup`):

```json
{
  "cupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "eventType": "CUP_STARTED",
  "timestamp": 1772768158123,
  "payload": {}
}
```

**Chat** (`/user/queue/chat`):

```json
{
  "chatId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
  "eventType": "MESSAGE_SENT",
  "timestamp": 1772768158123,
  "payload": {
    "senderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "content": "Buena mano!",
    "sentAt": 1772768158123
  }
}
```

Nota: los IDs de recurso (`matchId`, `leagueId`, `cupId`, `chatId`) son campos top-level del
evento, no dentro del `payload`.

### 9.5 eventType posibles — Match (`/user/queue/match`, 2 jugadores del partido)

- `AVAILABLE_ACTIONS_UPDATED`
- `CARD_PLAYED`
- `TURN_CHANGED`
- `TRUCO_CALLED`
- `TRUCO_RESPONDED`
- `ENVIDO_CALLED`
- `ENVIDO_RESOLVED`
- `SCORE_CHANGED`
- `ROUND_STARTED`
- `ROUND_ENDED`
- `GAME_STARTED`
- `GAME_SCORE_CHANGED`
- `MATCH_FINISHED`
- `FOLDED`
- `MATCH_FORFEITED`
- `PLAYER_HAND_UPDATED`
- `PLAYER_JOINED`
- `PLAYER_READY`
- `HAND_RESOLVED`
- `HAND_CHANGED`

### 9.5b eventType posibles — Liga (`/user/queue/league`, todos los participantes de la liga)

- `LEAGUE_PLAYER_JOINED` — un jugador se unió a la liga
- `LEAGUE_PLAYER_LEFT` — un jugador no-creador abandonó la liga
- `LEAGUE_CANCELLED` — la liga fue cancelada (creador abandona o timeout)
- `LEAGUE_STARTED` — la liga inició y se generaron los fixtures
- `LEAGUE_FIXTURE_ACTIVATED` — un fixture cambió de estado a PENDING
- `LEAGUE_MATCH_ACTIVATED` — un partido de liga fue creado y está listo
- `LEAGUE_ADVANCED` — un resultado fue registrado en la liga
- `LEAGUE_PLAYER_FORFEITED` — un jugador ha sido declarado forfeit
- `LEAGUE_FINISHED` — la liga terminó

### 9.5c eventType posibles — Copa (`/user/queue/cup`, todos los participantes de la copa)

- `CUP_PLAYER_JOINED` — un jugador se unió a la copa
- `CUP_PLAYER_LEFT` — un jugador no-creador abandonó la copa
- `CUP_CANCELLED` — la copa fue cancelada
- `CUP_STARTED` — la copa inició y se generó el bracket
- `CUP_BOUT_ACTIVATED` — un bout del bracket pasó a estado PENDING
- `CUP_MATCH_ACTIVATED` — un partido de copa fue creado y está listo
- `CUP_ADVANCED` — un resultado fue registrado y el bracket avanzó
- `CUP_PLAYER_FORFEITED` — un jugador fue declarado forfeit
- `CUP_FINISHED` — la copa terminó con un campeón

### 9.5d eventType posibles — Chat (`/user/queue/chat`, participantes del chat)

- `CHAT_CREATED` — chat creado automáticamente al iniciar match/liga/copa
- `MESSAGE_SENT` — un participante envió un mensaje

### 9.6 Payload por evento (resumen)

- `CARD_PLAYED`:
    - `{ seat, card: { suit, number } }`
- `HAND_RESOLVED`:
    - `{ cardPlayerOne, cardPlayerTwo, winnerSeat }`
- `TURN_CHANGED`:
    - `{ seat }`
- `TRUCO_CALLED`:
    - `{ callerSeat, call }`
- `TRUCO_RESPONDED`:
    - `{ responderSeat, response, call }`
- `ENVIDO_CALLED`:
    - `{ callerSeat, call }`
- `ENVIDO_RESOLVED`:
    - `{ response, winnerSeat, pointsMano?, pointsPie? }`
- `SCORE_CHANGED`:
    - `{ scorePlayerOne, scorePlayerTwo }`
- `ROUND_STARTED`:
    - `{ roundNumber, manoSeat }`
- `ROUND_ENDED`:
    - `{ winnerSeat }`
- `GAME_STARTED`:
    - `{ gameNumber }`
- `GAME_SCORE_CHANGED`:
    - `{ gamesWonPlayerOne, gamesWonPlayerTwo }`
- `MATCH_FINISHED`:
    - `{ winnerSeat, gamesWonPlayerOne, gamesWonPlayerTwo }`
- `FOLDED`:
    - `{ seat }`
- `MATCH_FORFEITED`:
    - `{ winnerSeat, loserSeat }`
- `PLAYER_HAND_UPDATED`:
    - `{ seat, cards: [{ suit, number }] }`
- `AVAILABLE_ACTIONS_UPDATED`:
    - `{ seat, availableActions: [{ type, parameter? }] }`
- `PLAYER_READY`:
    - `{ seat }`
- `PLAYER_JOINED`:
    - `{}`
- `HAND_CHANGED`:
    - actualmente no mapeado explicitamente en `MatchWsEvent`, por lo que puede llegar con
      `payload: {}`.
- `LEAGUE_MATCH_ACTIVATED`:
  - `{ matchId }` — se emite a todos los participantes de la liga cuando un partido
    es activado. El FE debe navegar o actualizar al nuevo partido usando el `matchId`.
- `CUP_MATCH_ACTIVATED`:
  - `{ matchId }` — se emite a todos los participantes de la copa cuando un partido de
    bracket es activado.
- `LEAGUE_PLAYER_JOINED` / `CUP_PLAYER_JOINED`:
  - `{ playerId }`
- `LEAGUE_PLAYER_LEFT` / `CUP_PLAYER_LEFT`:
  - `{ playerId }`
- `LEAGUE_CANCELLED` / `CUP_CANCELLED`:
  - `{}`
- `LEAGUE_STARTED` / `CUP_STARTED`:
  - `{}`
- `LEAGUE_FIXTURE_ACTIVATED`:
  - `{ fixtureId }`
- `CUP_BOUT_ACTIVATED`:
  - `{ boutId }`
- `LEAGUE_ADVANCED`:
  - `{ matchId?, winnerId }` — `matchId` puede ser `null` cuando el avance es automático
    (por ejemplo, forfeit del oponente)
- `CUP_ADVANCED`:
  - `{ matchId?, winnerId }` — `matchId` puede ser `null` cuando el avance es automático
    (por ejemplo, bye o forfeit del oponente)
- `LEAGUE_PLAYER_FORFEITED`:
  - `{ forfeiter }`
- `CUP_PLAYER_FORFEITED`:
  - `{ forfeiter }`
- `LEAGUE_FINISHED`:
  - `{ leaders: [playerId, ...] }`
- `CUP_FINISHED`:
  - `{ champion: playerId }`
- Nota: el `leagueId`/`cupId` se incluye como campo top-level del evento, no en el payload
- `CHAT_CREATED`:
  - `{}` — el FE puede consultar `GET /api/chats/by-parent/{parentType}/{parentId}` para obtener
    el chat
- `MESSAGE_SENT`:
  - `{ senderId, content, sentAt }` — `sentAt` en milisegundos epoch
  - Nota: el `chatId` se incluye como campo top-level del evento, no en el payload

## 10. Flujo de autenticacion recomendado

1. El FE llama a `/api/auth/register`, `/api/auth/login` o `/api/auth/guest` para obtener un JWT con
   el `playerId`.
2. Usa ese JWT como `Bearer` en todos los endpoints protegidos (`/api/matches/**`,
   `/api/leagues/**`, `/api/cups/**`).
3. Para WebSocket, envia el JWT en el header `Authorization` del frame STOMP `CONNECT`.

## 11. Notas para FE

- Tratar enums como case-sensitive.
- Manejar `204 No Content` en acciones de juego (sin body).
- Los eventos de liga (`LEAGUE_*`) llegan a **todos los participantes** de la liga, no solo a los
  dos jugadores de cada partido.
- Los eventos de copa (`CUP_*`) llegan a **todos los participantes** de la copa.
- En ligas, los partidos se crean **on-demand**: al iniciar la liga solo se crea el partido de la
  fecha 1. Cuando ese partido termina (o es forfeiteado), la liga activa automáticamente el
  siguiente partido elegible y envía un evento `LEAGUE_MATCH_ACTIVATED` a todos los participantes.
  Los fixtures con estado `SCHEDULED` son partidos futuros aún no creados.
- En copas, el bracket avanza automáticamente: cuando un partido termina se emite
  `CUP_MATCH_ACTIVATED` a todos los participantes con el siguiente partido a jugar.
  El FE puede consultar `GET /api/cups/{cupId}` para ver el estado completo del bracket.
- El **chat** se crea automáticamente al iniciar un match, liga o copa. Se elimina al finalizar
  o cancelarse el recurso padre. Para obtener el chat, usar
  `GET /api/chats/by-parent/{MATCH|LEAGUE|CUP}/{parentId}`. Los eventos de chat llegan por
  `/user/queue/chat`. El chat tiene un buffer circular de 50 mensajes y rate limit de 2 segundos
  entre mensajes del mismo jugador.

### 11.1 Reconexión WebSocket

Flujo recomendado para reconectar tras una desconexión:

1. Reconectar al WebSocket (`/ws` o `/ws-sockjs`) con el JWT en el frame STOMP `CONNECT`
2. Re-suscribirse a los canales relevantes (`/user/queue/match`, `/user/queue/league`, etc.)
3. **Bufferar** los eventos entrantes sin procesarlos todavía
4. Hacer `GET` del estado actual:

- Match: `GET /api/matches/{matchId}`
- Liga: `GET /api/leagues/{leagueId}`
- Copa: `GET /api/cups/{cupId}`
- Chat: `GET /api/chats/by-parent/{parentType}/{parentId}`

5. Aplicar el estado del GET como base autoritativa
6. Descartar eventos bufferados con `timestamp` anterior al GET; aplicar los posteriores
