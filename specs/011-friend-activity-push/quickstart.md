# Quickstart: Actividad en vivo de amigos

## Prerrequisitos

- Backend levantado localmente.
- Tres usuarios registrados: `ana`, `martina` y `agus`.
- `ana` y `martina` son amigas aceptadas.
- `ana` y `agus` no son amigos aceptados.
- Tokens JWT vigentes para los usuarios de prueba.

## Validacion manual

### 1. Bootstrap de amigos

1. Autenticarse como `ana`.
2. Ejecutar `GET /api/social/friends`.
3. Verificar que `martina` aparezca en la lista.
4. Verificar que `spectatableMatch` sea `null` si `martina` no esta jugando.

### 2. Snapshot al suscribirse a social

1. Conectar STOMP como `ana`.
2. Suscribirse a `/user/queue/social`.
3. Verificar que llega `FRIEND_ACTIVITY_STATE`.
4. Verificar que el payload contiene solo amistades aceptadas de `ana`.

### 3. Amigo empieza a jugar

1. Mantener a `ana` suscripta a `/user/queue/social`.
2. Hacer que `martina` inicie una partida que quede `IN_PROGRESS`.
3. Verificar que `ana` recibe `FRIEND_ACTIVITY_CHANGED`.
4. Verificar que el payload trae `friendUsername: "martina"` y `spectatableMatch.id`.
5. Usar ese `id` para iniciar spectate por `/user/queue/match-spectate`.

### 4. Amigo deja de jugar

1. Mantener a `ana` suscripta a `/user/queue/social`.
2. Finalizar, cancelar, abandonar o forfeitear la partida de `martina`.
3. Verificar que `ana` recibe `FRIEND_ACTIVITY_CHANGED` con `spectatableMatch: null`.
4. Verificar que la UI oculta la accion de spectate para `martina`.

### 5. Usuario no amigo

1. Mantener a `ana` suscripta a `/user/queue/social`.
2. Hacer que `agus` inicie una partida.
3. Verificar que `ana` no recibe actividad de `agus`.

### 6. Reconexion

1. Conectar `ana` y suscribirse a `/user/queue/social`.
2. Iniciar una partida con `martina`.
3. Cortar la conexion de `ana`.
4. Cambiar el estado de la partida.
5. Reconectar y suscribirse otra vez.
6. Verificar que `FRIEND_ACTIVITY_STATE` refleja el estado actual sin depender de eventos perdidos.

## Validacion automatizada sugerida

- `FriendActivityResolverTest`: arma snapshot solo con amistades aceptadas y sin datos privados.
- `GetFriendActivityQueryHandlerTest`: rechaza usuarios no registrados y devuelve actividad vigente.
- `FriendActivityMatchEventTranslatorTest`: publica alta al iniciar match y baja al terminarlo.
- `SocialSubscribeEventListenerTest`: envia `FRIEND_ACTIVITY_STATE` al suscribirse a
  `/user/queue/social`.
- Test de contrato social: documenta y verifica `FRIEND_ACTIVITY_STATE` y
  `FRIEND_ACTIVITY_CHANGED`.

## Comandos

```bash
.\gradlew.bat test
```
