# Quickstart: Espectar partidas de amigos

## Prerrequisitos

- Backend corriendo localmente.
- Tres usuarios registrados:
    - `ana`
    - `martina`
    - `agus`
- `ana` y `martina` son amigas confirmadas.
- `ana` y `agus` no son amigos confirmados.
- `martina` está jugando un match `IN_PROGRESS`.

## Flujo feliz

1. Autenticarse como `ana`.
2. Consultar `GET /api/social/friends`.
3. Verificar que la entrada de `martina` incluye:

```json
{
  "friendUsername": "martina",
  "spectatableMatch": {
    "id": "<matchId>",
    "status": "IN_PROGRESS"
  }
}
```

4. Conectar STOMP con el JWT de `ana`.
5. Suscribirse a `/user/queue/match-spectate` enviando header nativo `matchId: <matchId>`.
6. Verificar que llega `SPECTATE_STATE`.
7. Verificar que el payload de espectador no incluye cartas no jugadas ni acciones disponibles de
   jugadores.
8. Consultar `GET /api/matches/{matchId}/spectate` con el JWT de `ana`.
9. Verificar que devuelve el snapshot del mismo estado de espectador.

## Sin amistad confirmada

1. Autenticarse como `agus`.
2. Intentar suscribirse a `/user/queue/match-spectate` con el `matchId` de la partida de `martina`.
3. Verificar que llega `SPECTATE_ERROR`.
4. Verificar que `GET /api/matches/{matchId}/spectate` no devuelve snapshot de espectador.

## Match finalizado entre lista y suscripción

1. Autenticarse como `ana`.
2. Consultar `GET /api/social/friends` y guardar `spectatableMatch.id`.
3. Finalizar el match antes de suscribirse.
4. Suscribirse a `/user/queue/match-spectate` con ese `matchId`.
5. Verificar que llega `SPECTATE_ERROR` por match no disponible.

## Amistad removida durante la sesión

1. `ana` entra como espectadora de la partida de `martina`.
2. Se elimina la amistad entre `ana` y `martina`.
3. Verificar que la sesión de espectador de `ana` se corta si no pertenece a la misma liga/copa del
   match.
4. Reintentar suscripción con el mismo `matchId`.
5. Verificar que el nuevo intento devuelve `SPECTATE_ERROR`.

## Pruebas automatizadas esperadas

- `SpectatingEligibilityPolicyTest`: permite spectate si hay amistad aceptada con un jugador del
  match; rechaza pendiente/removida; conserva liga/copa.
- `SpectateMatchCommandHandlerTest`: registra spectatorship y publica conteo para amigo confirmado;
  rechaza usuario sin elegibilidad.
- `SpectatorCleanupOnFriendshipRemovedEventHandlerTest`: corta una sesión habilitada solo por
  amistad y conserva una sesión que sigue habilitada por liga/copa.
- `GetFriendsQueryHandlerTest`: devuelve `spectatableMatch` para amigo con match `IN_PROGRESS` y
  `null` para amigo sin partida espectable.
- `FriendshipControllerTest`: serializa `spectatableMatch`.
- `SpectateSubscribeEventListenerTest`: convierte rechazos de elegibilidad en `SPECTATE_ERROR`.

## Comandos

```powershell
.\gradlew.bat test
```

## Documentación

Actualizar y revisar:

- `docs/CONTRATOS_API.md`
- `README.md`
