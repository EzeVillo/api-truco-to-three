# Quickstart: Estado de envio en chat

## Validacion automatizada

1. Ejecutar el suite completo:

   ```powershell
   .\gradlew.bat test
   ```

2. Verificar casos especificos esperados:

   ```powershell
   .\gradlew.bat test --tests "*ChatTest"
   .\gradlew.bat test --tests "*GetChatMessagesQueryHandlerTest"
   .\gradlew.bat test --tests "*GetChatByParentQueryHandlerTest"
   .\gradlew.bat test --tests "*SendMessageCommandHandlerTest"
   .\gradlew.bat test --tests "*ChatControllerTest"
   ```

3. Confirmar que `CleanArchitectureTest` sigue pasando y que JaCoCo conserva el minimo de 70%.

## Validacion manual REST - Refresh de chat

1. Iniciar la aplicacion y autenticar dos usuarios participantes de un chat.
2. Enviar un mensaje con el usuario A.
3. Antes de que pasen 2 segundos, simular refresh consultando:

   ```http
   GET /api/chats/{chatId}/messages
   Authorization: Bearer <jwt-a>
   ```

4. Confirmar que la respuesta incluye:

   ```json
   {
     "sendState": {
       "canSendNow": false,
       "nextMessageAllowedAt": 1772768160123
     }
   }
   ```

5. Esperar hasta despues de `nextMessageAllowedAt` y repetir el GET.
6. Confirmar que `sendState.canSendNow = true` y `nextMessageAllowedAt = null`.

## Validacion manual REST - Envio a chat conocido

1. Enviar un mensaje:

   ```http
   POST /api/chats/{chatId}/messages
   Authorization: Bearer <jwt-a>
   Content-Type: application/json

   { "content": "Buena mano!" }
   ```

2. Confirmar respuesta `200` con `chatId` y `sendState`.
3. Confirmar que ya no responde `204 No Content`.
4. Confirmar que el mensaje sigue llegando por `/user/queue/chat` como `MESSAGE_SENT`.

## Validacion manual REST - Envio por recurso padre

1. Enviar un mensaje por recurso padre:

   ```http
   POST /api/chats/by-parent/FRIENDSHIP/{friendshipId}/messages
   Authorization: Bearer <jwt-a>
   Content-Type: application/json

   { "content": "Hola!" }
   ```

2. Confirmar respuesta `201` con `chatId` y `sendState`.
3. Si es el primer mensaje de una amistad aceptada, confirmar que el `chatId` permite navegar al
   chat.

## Validacion de rate limit

1. Enviar dos mensajes del mismo usuario con menos de 2 segundos de diferencia.
2. Confirmar que el segundo intento falla con error distinguible de rate limit.
3. Confirmar que el error no requiere `nextMessageAllowedAt` ni `retryAfterMs`.
4. Consultar el chat por GET y confirmar que `sendState` indica cuando se puede volver a enviar.

## Validacion de privacidad y compatibilidad

1. Con usuario B suscripto a `/user/queue/chat`, hacer que usuario A envie un mensaje.
2. Confirmar que B recibe el mensaje por `MESSAGE_SENT`.
3. Confirmar que el evento no contiene cuenta regresiva ni `sendState` de A.
4. Confirmar que usuarios no participantes siguen sin poder leer ni enviar mensajes en el chat.
