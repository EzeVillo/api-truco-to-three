# Research: Estado de envio en chat

## Decision: Exponer estado de envio en el snapshot REST de chat

**Decision**: Agregar un objeto `sendState` a las respuestas de lectura de chat para el jugador
autenticado. Este objeto indica `canSendNow` y, cuando esta en cooldown, `nextMessageAllowedAt`.

**Rationale**: El caso critico es el refresh: despues de recargar la aplicacion, el frontend no
conserva la confirmacion de envio anterior. La lectura del chat debe ser suficiente para reconstruir
si el jugador puede escribir ahora.

**Alternatives considered**:

- Informar el cooldown solo en el error por rate limit: descartado porque obliga al cliente a fallar
  un envio para recuperar estado.
- Informar solo en el ultimo mensaje propio: descartado porque duplica regla de negocio en el FE y
  se vuelve fragil si cambia el cooldown.
- Crear endpoint dedicado de cooldown: descartado por YAGNI; el estado pertenece naturalmente al
  chat que el cliente ya carga.

## Decision: Devolver estado actualizado en confirmaciones de envio

**Decision**: Las respuestas de envio exitoso deben incluir el mismo `sendState` actualizado para el
remitente. `POST /api/chats/{chatId}/messages` deja de responder sin body y pasa a confirmar con
estado. `POST /api/chats/by-parent/{parentType}/{parentId}/messages` mantiene `chatId` y suma
`sendState`.

**Rationale**: Tras enviar, el cliente necesita actualizar la UI sin hacer un GET inmediato. Usar el
mismo shape que el snapshot reduce divergencias.

**Alternatives considered**:

- Mantener `204 No Content` y requerir que el FE calcule 2 segundos: descartado porque contradice la
  necesidad de evitar inferencias fragiles.
- Requerir GET despues de cada envio: descartado por latencia y carga innecesaria.
- Usar solo el evento `MESSAGE_SENT`: descartado porque el evento es compartido con otros
  participantes y no debe transportar estado privado de accion del remitente.

## Decision: Mantener el error de rate limit como motivo distinguible sin metadata de cooldown

**Decision**: El error por rate limit debe seguir siendo identificable por codigo estable, pero no
debe requerir `nextMessageAllowedAt` ni `retryAfterMs`.

**Rationale**: El usuario aclaro que no hace falta avisar en ese error cuando se puede volver a
enviar. El flujo correcto para reconciliar es leer el estado del chat. Esto mantiene el error simple
y evita dos fuentes de verdad.

**Alternatives considered**:

- Agregar `retryAfterMs` al error: descartado por duplicar el estado de chat y porque no resuelve
  refresh.
- Cambiar semanticamente a `429 Too Many Requests`: descartado para este alcance; puede evaluarse en
  otra feature si se quiere normalizar rate limits HTTP.

## Decision: Usar epoch millis para `nextMessageAllowedAt`

**Decision**: Representar `nextMessageAllowedAt` como milisegundos epoch.

**Rationale**: El contrato existente usa epoch millis en eventos y mensajes de chat (`sentAt`). El
FE puede comparar contra su reloj y recalcular la cuenta regresiva localmente.

**Alternatives considered**:

- ISO-8601: descartado por inconsistencia con `sentAt` y otros eventos de chat.
- Duracion relativa solamente: descartado porque se degrada con latencia y re-renderizados.
- Enviar ambos timestamp y duracion: descartado por complejidad innecesaria para v1.

## Decision: No emitir countdown ni metadata de cooldown a otros participantes

**Decision**: No se agregan eventos periodicos ni cambios obligatorios a `MESSAGE_SENT` para
informar cooldown del remitente a todos los participantes.

**Rationale**: El cooldown limita solo la accion del usuario autenticado. Los demas participantes no
necesitan saber cuando ese usuario puede volver a escribir. Mantener el canal de chat sin ruido
reduce acoplamiento y carga.

**Alternatives considered**:

- Emitir eventos `CHAT_SEND_STATE_CHANGED`: descartado porque el cliente puede derivar el tiempo
  desde el snapshot o la confirmacion.
- Agregar `nextMessageAllowedAt` al payload `MESSAGE_SENT`: descartado porque expone estado de
  accion
  que solo necesita el remitente.

## Decision: Calcular el estado desde el agregado existente

**Decision**: Reutilizar `lastMessageTimestamps` y `rateLimitCooldown` de `Chat` para calcular
`canSendNow` y `nextMessageAllowedAt`.

**Rationale**: El agregado ya contiene la fuente autoritativa para validar rate limit. Exponer una
lectura calculada evita nueva persistencia y mantiene una sola regla.

**Alternatives considered**:

- Persistir un campo derivado `nextMessageAllowedAt`: descartado porque se puede calcular.
- Calcular exclusivamente en aplicacion a partir de mensajes: descartado porque no necesariamente
  refleja el mapa autoritativo de ultimo envio por jugador.
