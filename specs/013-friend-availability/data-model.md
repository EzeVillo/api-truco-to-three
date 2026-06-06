# Data Model: Disponibilidad de amigos para invitaciones

## Entidades y modelos

### FriendAvailability

Modelo de lectura que representa el estado visible de un amigo aceptado desde la perspectiva del
jugador autenticado.

**Campos**:

- `friendUsername`: username publico del amigo.
- `online`: boolean. `true` si el amigo tiene al menos una sesion activa conocida.
- `availability`: `AVAILABLE` o `BUSY`.
- `busyReason`: nullable. Motivo principal cuando `availability` es `BUSY`.
- `spectatableMatch`: nullable. Referencia minima a una partida que el jugador puede mirar.

**Reglas**:

- Solo existe para amistades `ACCEPTED`.
- `busyReason` debe ser `null` si `availability` es `AVAILABLE`.
- `busyReason` debe ser no-null si `availability` es `BUSY`.
- `online` no modifica por si mismo `availability`.
- `spectatableMatch` puede ser no-null aunque `availability` sea `BUSY`.
- No incluye cartas, acciones disponibles ni estado privado de ronda.

### AvailabilityStatus

Enum de disponibilidad para invitar.

**Valores**:

- `AVAILABLE`: ninguna regla vigente bloquea invitar o aceptar invitacion.
- `BUSY`: existe al menos una regla vigente que bloquea invitar o aceptar invitacion.

### BusyReason

Motivo principal por el cual un amigo esta ocupado.

**Valores iniciales**:

- `IN_MATCH`: tiene una partida no finalizada.
- `IN_LEAGUE`: participa en una liga activa, pendiente o en espera que bloquea invitaciones.
- `IN_CUP`: participa en una copa activa, pendiente o en espera que bloquea invitaciones.
- `OPEN_REMATCH`: tiene una sesion de revancha abierta.
- `IN_QUICK_QUEUE`: esta en cola de Quick Match.
- `PENDING_INVITATION`: existe una invitacion pendiente que bloquea la invitacion en ese alcance.
- `PENDING_FRIEND_REQUEST`: existe una solicitud de amistad pendiente que bloquea la accion social
  en ese alcance.
- `UNKNOWN`: hay un bloqueo real no clasificable con los motivos anteriores.

**Reglas**:

- Una invitacion o solicitud pendiente solo usa motivo si realmente bloquea.
- Si hay multiples motivos, se elige uno por prioridad deterministica.
- No debe exponer identificadores internos salvo que sean necesarios para una accion visible.

### OnlinePresence

Lectura de sesiones activas por jugador.

**Campos**:

- `playerId`: jugador.
- `activeSessionCount`: cantidad de sesiones activas conocidas.
- `online`: derivado de `activeSessionCount > 0`.

**Reglas**:

- Multiples pestanas o dispositivos cuentan como multiples sesiones.
- El jugador queda online mientras al menos una sesion siga activa.
- La desconexion puede ser eventual; el snapshot debe corregir estados obsoletos.
- No se persiste como fuente historica.

### SpectatableMatchRef

Referencia minima de una partida disponible para spectate.

**Campos**:

- `id`: identificador del match.
- `status`: estado visible del match; para spectate social se espera `IN_PROGRESS`.

**Reglas**:

- El `id` permite iniciar el flujo existente de spectate.
- No contiene cartas, acciones disponibles, puntaje de ronda ni informacion privada.
- Si el match deja de ser espectable, la referencia pasa a `null`.

### FriendAvailabilityState

Snapshot completo de disponibilidad social para el jugador autenticado.

**Campos**:

- `friends`: lista de `FriendAvailability`.

**Reglas**:

- Puede reemplazar el estado local completo del cliente.
- Cada `friendUsername` aparece como maximo una vez.
- Solo incluye amistades aceptadas vigentes.

### FriendAvailabilityChange

Delta de disponibilidad para un unico amigo.

**Campos**:

- `friendUsername`: username del amigo cuyo estado cambio.
- `online`: nuevo estado online.
- `availability`: nuevo estado de disponibilidad.
- `busyReason`: nuevo motivo principal o `null`.
- `spectatableMatch`: nueva referencia espectable o `null`.

**Reglas**:

- Es idempotente: aplicar el mismo delta mas de una vez deja el mismo estado.
- Reemplaza el estado anterior de ese amigo.
- Solo se envia a usuarios con amistad aceptada vigente.

## Transiciones de estado

```text
Amigo disponible
  -> entra a match/liga/copa/quick queue/revancha o pendiente bloqueante
  -> availability = BUSY, busyReason = motivo principal
  -> se publica FRIEND_AVAILABILITY_CHANGED

Amigo ocupado
  -> desaparece su ultimo bloqueo real
  -> availability = AVAILABLE, busyReason = null
  -> se publica FRIEND_AVAILABILITY_CHANGED

Amigo offline
  -> abre al menos una sesion autenticada
  -> online = true
  -> se publica FRIEND_AVAILABILITY_CHANGED

Amigo online
  -> cierra/expira su ultima sesion autenticada
  -> online = false
  -> se publica FRIEND_AVAILABILITY_CHANGED

Usuario se suscribe a social
  -> backend calcula FriendAvailabilityState
  -> se publica FRIEND_AVAILABILITY_STATE
  -> cliente reemplaza estado local

Amistad aceptada se elimina
  -> evento social existente remueve la amistad
  -> no se publican mas deltas de disponibilidad entre esos usuarios
```

## Invariantes

- La disponibilidad social no registra espectadores.
- La presencia online no se usa como bloqueo.
- Un usuario solo recibe disponibilidad de amistades aceptadas vigentes.
- El estado social nunca muestra informacion privada de partida.
- Un cambio de disponibilidad reemplaza por completo el item anterior del amigo.
- La regla de truco-to-three no cambia: partida a exactamente 3 puntos, pasarse de 3 pierde, series
  mejor de 1, 3 o 5 cuando aplique.
