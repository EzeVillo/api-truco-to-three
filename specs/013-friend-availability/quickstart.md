# Quickstart: Disponibilidad de amigos para invitaciones

## Validacion automatizada

1. Ejecutar el suite completo:

   ```powershell
   .\gradlew.bat test
   ```

2. Verificar casos especificos esperados:

   ```powershell
   .\gradlew.bat test --tests "*FriendAvailabilityResolverTest"
   .\gradlew.bat test --tests "*FriendAvailabilityContractTest"
   .\gradlew.bat test --tests "*SocialSubscribeEventListenerTest"
   .\gradlew.bat test --tests "*StompFriendAvailabilityNotificationHandlerTest"
   ```

3. Confirmar que `CleanArchitectureTest` sigue pasando y que JaCoCo conserva el minimo de 70%.

## Validacion manual REST

1. Iniciar la aplicacion y autenticar dos usuarios amigos aceptados.
2. Con el usuario A, consultar:

   ```http
   GET /api/social/friendships
   Authorization: Bearer <jwt-a>
   ```

3. Confirmar que cada amigo incluye:

    - `friendUsername`
    - `online`
    - `availability`
    - `busyReason`
    - `spectatableMatch`

4. Poner al usuario B en cada estado bloqueante y repetir la consulta:

    - partida no finalizada
    - liga activa/en espera
    - copa activa/en espera
    - revancha abierta
    - cola de Quick Match
    - invitacion o solicitud pendiente solo si bloquea segun contrato

5. Confirmar que B vuelve a `AVAILABLE` cuando se libera el ultimo bloqueo.

## Validacion manual WebSocket

1. Conectar STOMP con el JWT del usuario A.
2. Suscribirse a `/user/queue/social`.
3. Confirmar recepcion de `FRIEND_AVAILABILITY_STATE`.
4. Cambiar el estado del usuario B y confirmar `FRIEND_AVAILABILITY_CHANGED`.
5. Abrir una segunda sesion de B y confirmar que `online` permanece `true` al cerrar solo una.
6. Cerrar o expirar la ultima sesion de B y confirmar que `online` pasa a `false`.

## Validacion de privacidad

1. Eliminar la amistad entre A y B.
2. Cambiar disponibilidad u online de B.
3. Confirmar que A no recibe nuevos eventos de disponibilidad de B.
4. Crear una solicitud de amistad pendiente entre A y B.
5. Confirmar que no se expone disponibilidad ni online hasta que la amistad sea aceptada.

## Validacion de spectate

1. Crear una partida espectable de B.
2. Confirmar que `spectatableMatch.id` aparece en la lista de A.
3. Confirmar que iniciar spectate sigue usando `/user/queue/match-spectate` con header `matchId`.
4. Confirmar que disponibilidad para invitar y accion de mirar se renderizan como decisiones
   separadas.
