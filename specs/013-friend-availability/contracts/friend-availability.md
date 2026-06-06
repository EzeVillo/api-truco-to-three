# Contrato: Disponibilidad de amigos para invitaciones

## Resumen

La lista social expone disponibilidad para invitar, presencia online aproximada y partida espectable
opcional para cada amigo aceptado. La feature no cambia el alta de espectador ni las reglas de
creacion/aceptacion de invitaciones: solo publica una vista anticipada del resultado.

## REST - Bootstrap de amigos

### `GET /api/social/friendships`

Devuelve los amigos aceptados del usuario autenticado con disponibilidad, online y referencia
espectable actual.

#### Respuesta 200

```json
[
  {
    "friendUsername": "martina",
    "online": true,
    "availability": "BUSY",
    "busyReason": "IN_MATCH",
    "spectatableMatch": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "IN_PROGRESS"
    }
  },
  {
    "friendUsername": "agus",
    "online": false,
    "availability": "AVAILABLE",
    "busyReason": null,
    "spectatableMatch": null
  }
]
```

#### Campos

| Campo              | Tipo                      | Descripcion                                          |
|--------------------|---------------------------|------------------------------------------------------|
| `friendUsername`   | string                    | Username publico del amigo aceptado.                 |
| `online`           | boolean                   | Presencia aproximada por sesiones activas conocidas. |
| `availability`     | `AVAILABLE` / `BUSY`      | Si el amigo puede ser invitado o aceptar invitacion. |
| `busyReason`       | `BusyReason` / null       | Motivo principal cuando `availability = BUSY`.       |
| `spectatableMatch` | `SpectatableMatch` / null | Partida espectable opcional.                         |

#### `BusyReason`

Valores iniciales:

- `IN_MATCH`
- `IN_LEAGUE`
- `IN_CUP`
- `OPEN_REMATCH`
- `IN_QUICK_QUEUE`
- `PENDING_INVITATION`
- `PENDING_FRIEND_REQUEST`
- `UNKNOWN`

#### Reglas

- `busyReason` es `null` cuando `availability = AVAILABLE`.
- `busyReason` es obligatorio cuando `availability = BUSY`.
- `online` no implica disponibilidad ni ocupacion.
- `spectatableMatch.id` se usa como header `matchId` al suscribirse a
  `/user/queue/match-spectate`.
- La respuesta solo incluye amistades aceptadas vigentes.
- La respuesta no incluye cartas, acciones privadas ni estado de ronda.

## WebSocket - Cola social

### Destino

`/user/queue/social`

### Evento `FRIEND_AVAILABILITY_STATE`

Snapshot completo enviado al usuario autenticado al suscribirse a la cola social.

```json
{
  "eventType": "FRIEND_AVAILABILITY_STATE",
  "timestamp": 1791234567890,
  "payload": {
    "friends": [
      {
        "friendUsername": "martina",
        "online": true,
        "availability": "BUSY",
        "busyReason": "IN_MATCH",
        "spectatableMatch": {
          "id": "550e8400-e29b-41d4-a716-446655440000",
          "status": "IN_PROGRESS"
        }
      },
      {
        "friendUsername": "agus",
        "online": false,
        "availability": "AVAILABLE",
        "busyReason": null,
        "spectatableMatch": null
      }
    ]
  }
}
```

#### Reglas

- `friends` reemplaza el estado local completo de disponibilidad social del cliente.
- Cada `friendUsername` aparece como maximo una vez.
- Solo incluye amistades aceptadas vigentes.
- Debe omitirse toda informacion privada del match.

### Evento `FRIEND_AVAILABILITY_CHANGED`

Delta enviado cuando cambia `online`, `availability`, `busyReason` o `spectatableMatch` de un amigo.

```json
{
  "eventType": "FRIEND_AVAILABILITY_CHANGED",
  "timestamp": 1791234567999,
  "payload": {
    "friendUsername": "martina",
    "online": true,
    "availability": "AVAILABLE",
    "busyReason": null,
    "spectatableMatch": null
  }
}
```

#### Reglas

- El evento se envia solo a amigos aceptados vigentes del usuario cuyo estado cambio.
- El delta reemplaza el item completo del amigo.
- El evento es idempotente.
- Si la amistad deja de estar aceptada, el cliente debe remover al amigo por el evento social de
  amistad existente y no esperar nuevos deltas de disponibilidad.

## Compatibilidad

- Los eventos sociales existentes se mantienen.
- `FRIEND_ACTIVITY_STATE` y `FRIEND_ACTIVITY_CHANGED` pueden mantenerse durante una ventana de
  compatibilidad si el frontend actual aun los consume; el contrato nuevo recomendado es
  `FRIEND_AVAILABILITY_STATE` / `FRIEND_AVAILABILITY_CHANGED`.
- El contrato de `/user/queue/match-spectate` no cambia.
- `GET /api/matches/{matchId}/spectate` sigue funcionando solo para espectadores ya registrados.

## Privacidad

- No se envian cartas no jugadas.
- No se envian acciones disponibles por asiento.
- No se envia estado privado de ronda.
- No se envia disponibilidad ni online para solicitudes pendientes, rechazadas, canceladas o
  amistades removidas.
- `online` es visible solo para amistades aceptadas vigentes.

## Flujo recomendado del cliente

1. Cargar `GET /api/social/friendships` para render inicial.
2. Conectar STOMP con JWT vigente.
3. Suscribirse a `/user/queue/social`.
4. Aplicar `FRIEND_AVAILABILITY_STATE` como reconciliacion completa cuando llegue.
5. Aplicar `FRIEND_AVAILABILITY_CHANGED` como reemplazo del item de un amigo.
6. Habilitar invitacion solo cuando `availability = AVAILABLE`.
7. Para mirar una partida, usar el flujo existente `/user/queue/match-spectate` con header
   `matchId = spectatableMatch.id`.
