# Contrato: Actividad en vivo de amigos

## Resumen

La feature agrega actualizaciones en vivo de disponibilidad de spectate para amigos confirmados. No
crea un flujo nuevo para mirar partidas: solo descubre y actualiza el `matchId` espectable.

## REST - Bootstrap de amigos

### `GET /api/social/friends`

Devuelve los amigos confirmados del usuario autenticado con la referencia espectable actual, si
existe.

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

- `spectatableMatch` es `null` cuando el amigo no tiene partida espectable.
- `spectatableMatch.id` es el `matchId` para iniciar el flujo existente de spectate.
- La respuesta solo incluye amistades aceptadas.
- La respuesta no incluye cartas, acciones privadas ni estado de ronda.

## WebSocket - Cola social

### Destino

`/user/queue/social`

El cliente usa la misma cola social existente para amistades, invitaciones y actividad de amigos.

### Evento `FRIEND_ACTIVITY_STATE`

Snapshot de actividad enviado al usuario autenticado al suscribirse a la cola social. Sirve para
reconciliar la vista despues del bootstrap inicial o una reconexion.

```json
{
  "eventType": "FRIEND_ACTIVITY_STATE",
  "timestamp": 1791234567890,
  "payload": {
    "friends": [
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
  }
}
```

#### Reglas

- `friends` reemplaza el estado local completo de actividad social del cliente.
- Cada `friendUsername` aparece como maximo una vez.
- Solo incluye amistades aceptadas vigentes.
- Debe omitirse toda informacion privada del match.

### Evento `FRIEND_ACTIVITY_CHANGED`

Delta enviado cuando cambia la disponibilidad espectable de un amigo.

#### Amigo empieza a tener partida espectable

```json
{
  "eventType": "FRIEND_ACTIVITY_CHANGED",
  "timestamp": 1791234567890,
  "payload": {
    "friendUsername": "martina",
    "spectatableMatch": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "IN_PROGRESS"
    }
  }
}
```

#### Amigo deja de tener partida espectable

```json
{
  "eventType": "FRIEND_ACTIVITY_CHANGED",
  "timestamp": 1791234567999,
  "payload": {
    "friendUsername": "martina",
    "spectatableMatch": null
  }
}
```

#### Reglas

- El evento se envia solo a amigos aceptados vigentes de los jugadores del match.
- `spectatableMatch` no-null significa que el cliente puede mostrar la accion de spectate.
- `spectatableMatch: null` significa que el cliente debe ocultar o deshabilitar la accion de
  spectate para ese amigo.
- El evento es idempotente: aplicar el mismo delta mas de una vez no cambia el resultado final.

## Flujo recomendado del cliente

1. Cargar `GET /api/social/friends` para render inicial.
2. Conectar STOMP con JWT vigente.
3. Suscribirse a `/user/queue/social`.
4. Aplicar `FRIEND_ACTIVITY_STATE` como reconciliacion completa cuando llegue.
5. Aplicar `FRIEND_ACTIVITY_CHANGED` como delta por amigo.
6. Para mirar una partida, suscribirse al flujo existente `/user/queue/match-spectate` con header
   `matchId`.

## Compatibilidad

- Los eventos sociales existentes se mantienen sin cambios.
- El contrato de `/user/queue/match-spectate` no cambia.
- `GET /api/matches/{matchId}/spectate` sigue funcionando solo para espectadores ya registrados.
- No hay endpoint REST nuevo para alta de espectador.

## Privacidad

- No se envian cartas no jugadas.
- No se envian acciones disponibles por asiento.
- No se envia estado privado de ronda.
- No se envia actividad para solicitudes pendientes, rechazadas, canceladas o amistades removidas.
