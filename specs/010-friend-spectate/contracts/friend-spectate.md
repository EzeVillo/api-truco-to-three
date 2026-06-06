# Contrato: Espectar partidas de amigos

## Resumen

La feature reutiliza el contrato actual de spectate y agrega una forma social de descubrir partidas
espectables de amigos.

## REST - Lista de amigos

### `GET /api/social/friends`

Devuelve los amigos confirmados del usuario autenticado. Cada amigo puede incluir una referencia de
partida espectable.

#### Respuesta 200

```json
[
  {
    "friendUsername": "martina",
    "spectatableMatch": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "IN_PROGRESS"
    }
  },
  {
    "friendUsername": "agus",
    "spectatableMatch": null
  }
]
```

#### Reglas

- `spectatableMatch` es `null` cuando el amigo no tiene partida `IN_PROGRESS`.
- `spectatableMatch` solo aparece para amistades aceptadas devueltas por este endpoint.
- `id` es el `matchId` que el cliente debe usar para iniciar el flujo de spectate existente.
- `status` usa el enum case-sensitive del contrato de match; para esta acción se espera
  `IN_PROGRESS`.
- La respuesta no incluye cartas, estado de ronda ni información privada del match.

## WebSocket - Alta de spectate

### `/user/queue/match-spectate`

Sin cambios de transporte. Para iniciar spectate de una partida de amigo:

1. Conectar STOMP con JWT vigente.
2. Suscribirse a `/user/queue/match-spectate`.
3. Enviar header nativo `matchId: <id de spectatableMatch>`.
4. Si la amistad aceptada sigue vigente y el match sigue `IN_PROGRESS`, el backend registra al
   espectador y envía `SPECTATE_STATE`.

#### Eventos existentes

- `SPECTATE_STATE`: snapshot inicial del estado de espectador.
- `SPECTATE_ERROR`: error de alta.
- `SPECTATOR_COUNT_CHANGED`: cambio de cantidad de espectadores.
- Eventos públicos de match reenviados a espectadores activos.

#### Nuevas reglas de elegibilidad

Además de liga/copa, el alta es válida si:

- el espectador es amigo confirmado de `playerOne` o `playerTwo`;
- el match está `IN_PROGRESS`;
- el espectador no es jugador del match;
- el espectador no está espectando otro match.

#### Errores esperados

- Match inexistente: evento `SPECTATE_ERROR`.
- Match no `IN_PROGRESS`: evento `SPECTATE_ERROR`.
- Sin amistad aceptada y sin pertenencia a liga/copa: evento `SPECTATE_ERROR`.
- El usuario intenta espectar su propio match: evento `SPECTATE_ERROR`.
- El usuario ya especta otro match: evento `SPECTATE_ERROR`.

## REST - Snapshot de espectador

### `GET /api/matches/{matchId}/spectate`

Sin cambios. Sigue funcionando solo si el usuario ya quedó registrado como espectador por el flujo
WebSocket.

## Compatibilidad

- Los espectadores de liga/copa siguen funcionando igual.
- No hay endpoint REST nuevo para alta.
- No hay preferencia para bloquear espectadores amigos.
- No se reenvían eventos privados por asiento a amigos espectadores.

## Invalidez por amistad removida

Cuando se elimina la amistad que habilitaba un spectate activo:

- el backend debe cortar la sesión de espectador si no queda otro motivo válido de elegibilidad;
- el conteo de espectadores debe actualizarse por el flujo existente;
- un nuevo intento de suscripción al mismo `matchId` debe responder `SPECTATE_ERROR` mientras no
  exista amistad aceptada ni pertenencia a liga/copa.
