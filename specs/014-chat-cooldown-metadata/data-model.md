# Data Model: Estado de envio en chat

## Entidades y modelos

### ChatSendState

Modelo de lectura que representa la capacidad actual del jugador autenticado para enviar mensajes en
un chat.

**Campos**:

- `canSendNow`: boolean. `true` si el jugador autenticado puede enviar un mensaje en este momento.
- `nextMessageAllowedAt`: nullable long epoch millis. Primer instante en el que el jugador puede
  enviar sin violar cooldown; `null` cuando `canSendNow = true`.

**Reglas**:

- Se calcula desde el ultimo envio registrado para el jugador autenticado y el cooldown vigente del
  chat.
- `nextMessageAllowedAt` debe ser no-null cuando `canSendNow = false`.
- `nextMessageAllowedAt` debe ser null cuando `canSendNow = true`.
- No contiene informacion de otros participantes.
- No reemplaza validaciones de permisos ni contenido; solo describe cooldown de envio.

### ChatMessagesState

Modelo de lectura completo del chat para el jugador autenticado.

**Campos**:

- `chatId`: identificador del chat.
- `parentType`: `MATCH`, `LEAGUE`, `CUP` o `FRIENDSHIP`.
- `parentId`: identificador del recurso padre.
- `messages`: lista de mensajes retenidos, maximo 50.
- `sendState`: `ChatSendState` del jugador autenticado.

**Reglas**:

- Solo se entrega a participantes del chat.
- Debe incluir `sendState` tanto si hay mensajes como si el chat esta vacio.
- El estado se calcula para quien hace la request, no para el ultimo remitente.
- La lista de mensajes mantiene el contrato actual de contenido y timestamps.

### SendMessageResult

Resultado de enviar un mensaje a un chat conocido.

**Campos**:

- `chatId`: identificador del chat donde se publico el mensaje.
- `sendState`: `ChatSendState` actualizado para el remitente despues del envio.

**Reglas**:

- Solo existe para envios aceptados.
- Despues de un envio aceptado, `canSendNow` normalmente es `false` y `nextMessageAllowedAt`
  representa el fin del cooldown.
- No incluye el mensaje completo; la distribucion del mensaje sigue por el flujo de chat existente.

### SendMessageToParentResult

Resultado de enviar un mensaje usando recurso padre.

**Campos**:

- `chatId`: identificador del chat existente o creado diferidamente.
- `sendState`: `ChatSendState` actualizado para el remitente.

**Reglas**:

- Para `FRIENDSHIP`, puede crear el chat diferidamente antes del primer mensaje.
- Para `MATCH`, `LEAGUE` y `CUP`, el chat debe existir previamente.
- Debe devolver el `chatId` para que el cliente pueda navegar o reconciliar sin GET adicional.

### ChatRateLimitRejection

Rechazo de un intento de envio demasiado rapido.

**Campos visibles**:

- `errorCode`: codigo estable que identifica rate limit.
- `message`: descripcion legible del rechazo.

**Reglas**:

- No es la fuente de verdad del cooldown.
- No requiere `nextMessageAllowedAt` ni `retryAfterMs`.
- El cliente puede hacer una lectura del chat si necesita reconciliar `sendState`.

## Transiciones de estado

```text
Jugador sin mensajes recientes
  -> obtiene estado del chat
  -> sendState.canSendNow = true
  -> sendState.nextMessageAllowedAt = null

Jugador envia mensaje valido
  -> dominio registra sentAt como ultimo envio del jugador
  -> respuesta incluye sendState.canSendNow = false
  -> respuesta incluye sendState.nextMessageAllowedAt = sentAt + cooldown

Jugador refresca antes de que termine el cooldown
  -> obtiene estado del chat
  -> backend calcula now < lastSent + cooldown
  -> sendState.canSendNow = false
  -> sendState.nextMessageAllowedAt = lastSent + cooldown

Cooldown vencido
  -> obtiene estado del chat
  -> backend calcula now >= lastSent + cooldown
  -> sendState.canSendNow = true
  -> sendState.nextMessageAllowedAt = null

Jugador intenta enviar durante cooldown
  -> dominio rechaza por rate limit
  -> error conserva codigo distinguible
  -> cliente puede reconciliar con GET de chat si lo necesita
```

## Invariantes

- La regla de cooldown sigue siendo minimo 2 segundos entre mensajes del mismo jugador.
- `sendState` siempre se calcula desde la perspectiva del jugador autenticado.
- Los eventos `MESSAGE_SENT` no transportan cuentas regresivas ni estado de accion privado.
- Otros participantes no reciben `sendState` del remitente por WebSocket.
- La feature no cambia retencion de mensajes, maximo de caracteres ni participacion requerida.
- La regla de truco-to-three no cambia: partida a exactamente 3 puntos, pasarse de 3 pierde, series
  mejor de 1, 3 o 5 cuando aplique.
