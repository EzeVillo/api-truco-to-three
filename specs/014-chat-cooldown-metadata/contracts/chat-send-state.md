# Contrato: Estado de envio en chat

## Resumen

Las lecturas de chat y las confirmaciones de envio exponen `sendState`, calculado para el jugador
autenticado. El objetivo es que el frontend sepa si puede enviar ahora, incluso despues de
refrescar,
sin forzar un intento que falle por rate limit.

## Modelo comun: `ChatSendState`

```json
{
  "canSendNow": false,
  "nextMessageAllowedAt": 1772768160123
}
```

| Campo                  | Tipo          | Descripcion                                                            |
|------------------------|---------------|------------------------------------------------------------------------|
| `canSendNow`           | boolean       | Si el jugador autenticado puede enviar un mensaje ahora.               |
| `nextMessageAllowedAt` | number / null | Epoch millis del proximo envio permitido; `null` si `canSendNow=true`. |

### Reglas

- `sendState` siempre corresponde al jugador autenticado de la request.
- `nextMessageAllowedAt` es obligatorio cuando `canSendNow=false`.
- `nextMessageAllowedAt` es `null` cuando `canSendNow=true`.
- El cliente puede mostrar cuenta regresiva local comparando `nextMessageAllowedAt` con su reloj.

## REST - Obtener mensajes por chatId

### `GET /api/chats/{chatId}/messages`

Auth: Bearer requerido.

#### Respuesta 200

```json
{
  "chatId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "parentType": "MATCH",
  "parentId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "sendState": {
    "canSendNow": true,
    "nextMessageAllowedAt": null
  },
  "messages": [
    {
      "messageId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
      "sender": "juancho",
      "content": "Buena mano!",
      "sentAt": 1772768158123
    }
  ]
}
```

#### Errores

- `404` si el chat no existe.
- `422` si el jugador no pertenece al chat.

## REST - Buscar chat por recurso padre

### `GET /api/chats/by-parent/{parentType}/{parentId}`

Auth: Bearer requerido.

Path params:

- `parentType`: `MATCH`, `LEAGUE`, `CUP` o `FRIENDSHIP`.
- `parentId`: UUID del match, liga, copa o amistad.

#### Respuesta 200

Misma estructura que `GET /api/chats/{chatId}/messages`, incluyendo `sendState`.

#### Errores

- `400` si `parentType` no coincide exactamente con `MATCH`, `LEAGUE`, `CUP` o `FRIENDSHIP`.
- `404` si no existe chat para ese recurso.
- `422` si el jugador no pertenece al chat.

## REST - Enviar mensaje

### `POST /api/chats/{chatId}/messages`

Auth: Bearer requerido.

Request:

```json
{
  "content": "Buena mano!"
}
```

#### Respuesta 200

```json
{
  "chatId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "sendState": {
    "canSendNow": false,
    "nextMessageAllowedAt": 1772768160123
  }
}
```

#### Reglas

- Cambia el contrato anterior de `204 No Content` a respuesta con body.
- `sendState` representa el estado del remitente despues del envio aceptado.
- El mensaje enviado sigue llegando por `/user/queue/chat` como `MESSAGE_SENT`.

#### Errores

- `404` si el chat no existe.
- `422` si el jugador no pertenece al chat, el mensaje esta vacio, excede 500 caracteres, o viola
  el rate limit de 2 segundos.
- Si el error es rate limit, el cuerpo de error mantiene `errorCode` distinguible, pero no necesita
  incluir `sendState`, `nextMessageAllowedAt` ni `retryAfterMs`.

## REST - Enviar mensaje por recurso padre

### `POST /api/chats/by-parent/{parentType}/{parentId}/messages`

Auth: Bearer requerido.

Path params:

- `parentType`: `MATCH`, `LEAGUE`, `CUP` o `FRIENDSHIP`.
- `parentId`: UUID del match, liga, copa o amistad.

Request:

```json
{
  "content": "Buena mano!"
}
```

#### Respuesta 201

```json
{
  "chatId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "sendState": {
    "canSendNow": false,
    "nextMessageAllowedAt": 1772768160123
  }
}
```

#### Reglas

- Para `FRIENDSHIP`, el chat puede crearse diferidamente en el primer mensaje.
- Para `MATCH`, `LEAGUE` y `CUP`, el chat ya debe existir.
- `chatId` permite navegar directamente al chat sin GET extra.
- `sendState` representa el estado del remitente despues del envio aceptado.

#### Errores

- `404` si el chat no existe para `MATCH`/`LEAGUE`/`CUP` o si la amistad no esta aceptada para
  `FRIENDSHIP`.
- `422` si el jugador no pertenece al chat, el mensaje esta vacio, excede 500 caracteres, o viola
  el rate limit de 2 segundos.

## WebSocket - Chat

### Destino

`/user/queue/chat`

### Regla de compatibilidad

El evento `MESSAGE_SENT` no cambia por esta feature. Los participantes reciben el mensaje en tiempo
real como hasta ahora. El estado `sendState` del jugador autenticado se obtiene por REST en lecturas
y confirmaciones de envio.

## Flujo recomendado del cliente

1. Cargar el chat con `GET /api/chats/{chatId}/messages` o
   `GET /api/chats/by-parent/{parentType}/{parentId}`.
2. Renderizar el input segun `sendState.canSendNow`.
3. Si `canSendNow=false`, calcular la cuenta regresiva local contra `nextMessageAllowedAt`.
4. Al enviar correctamente, actualizar el estado local con `sendState` de la respuesta.
5. Si un envio falla por rate limit y el cliente necesita reconciliar, volver a leer el chat.
6. Mantener la suscripcion `/user/queue/chat` para mensajes en tiempo real; no esperar countdowns.
