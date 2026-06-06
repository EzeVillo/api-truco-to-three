# Research: Disponibilidad de amigos para invitaciones

## Decision: Reutilizar la cola social para disponibilidad de amigos

**Decision**: Enviar snapshots y deltas de disponibilidad por `/user/queue/social`, junto con el
resto de novedades sociales.

**Rationale**: La disponibilidad es informacion social de amistades aceptadas. Ya existe la cola
personal social y ya se usa para amistad, invitaciones y actividad espectable, por lo que mantenerla
evita un canal nuevo y reduce suscripciones del cliente.

**Alternatives considered**:

- Crear `/user/queue/friend-availability`: descartado porque agrega un canal sin necesidad y duplica
  lifecycle de suscripcion.
- Reutilizar `/user/queue/presence`: descartado porque ese canal representa presencia del propio
  usuario, no informacion visible para terceros.

## Decision: `availability` representa capacidad real de invitacion

**Decision**: Marcar `BUSY` solo cuando una regla vigente bloquearia recibir o aceptar una
invitacion
a partida; marcar `AVAILABLE` cuando no hay bloqueo.

**Rationale**: La UI debe anticipar el resultado de negocio de invitar. Si una invitacion pendiente
o
solicitud no bloquea, mostrar ocupado seria falso y degradaria la experiencia.

**Alternatives considered**:

- Mostrar ocupado por cualquier actividad social: descartado porque mezcla informacion contextual
  con bloqueo real.
- Mostrar solo si esta en match: descartado porque liga, copa, cola quick match y revancha abierta
  tambien pueden bloquear.

## Decision: Motivo principal deterministico

**Decision**: Cuando un amigo tenga varios bloqueos, mostrar un unico `busyReason` elegido por orden
estable: `IN_MATCH`, `IN_LEAGUE`, `IN_CUP`, `OPEN_REMATCH`, `IN_QUICK_QUEUE`,
`PENDING_INVITATION`, `PENDING_FRIEND_REQUEST`, `UNKNOWN`.

**Rationale**: El cliente necesita un mensaje simple y no contradictorio. El orden prioriza estados
de juego activos/reconectables sobre pendientes sociales y deja `UNKNOWN` como ultimo recurso para
errores o bloqueos no clasificados.

**Alternatives considered**:

- Enviar todos los motivos: descartado por complejidad visual y porque la spec pide un motivo
  principal.
- Elegir aleatoriamente el primer bloqueo encontrado: descartado porque produciria cambios
  inestables entre snapshots.

## Decision: Online como presencia aproximada por sesiones activas

**Decision**: Calcular `online` desde sesiones WebSocket activas conocidas por jugador. Un jugador
esta online mientras tenga al menos una sesion activa; al desconectar o expirar la ultima sesion,
queda offline.

**Rationale**: Es el mecanismo ya disponible para presencia en vivo. No requiere persistencia y
admite multiples pestanas/dispositivos. La spec acepta que online sea aproximado y se corrija por
reconciliacion.

**Alternatives considered**:

- Persistir presencia en base de datos: descartado por YAGNI y por riesgo de estados obsoletos.
- Usar actividad REST reciente: descartado porque no refleja sesiones vivas ni desconexiones de WS.
- Exigir heartbeat custom: descartado inicialmente; STOMP/session lifecycle ya da senales
  suficientes para v1.

## Decision: Mantener `spectatableMatch` dentro del estado social

**Decision**: Incluir `spectatableMatch` como campo nullable del mismo item de disponibilidad.

**Rationale**: El usuario necesita decidir dos acciones relacionadas pero distintas: invitar o
mirar.
Mantenerlas en un solo item evita reconciliar dos listas por username.

**Alternatives considered**:

- Separar disponibilidad y actividad espectable en eventos independientes: descartado porque aumenta
  carreras de sincronizacion en el cliente.
- Reemplazar spectate por disponibilidad: descartado porque estar ocupado no implica que la partida
  sea espectable.

## Decision: Snapshot completo al suscribirse

**Decision**: Al suscribirse a `/user/queue/social`, enviar `FRIEND_AVAILABILITY_STATE` con todos
los
amigos aceptados y su estado actual.

**Rationale**: Cierra carreras entre bootstrap REST y deltas, y permite reconciliar despues de una
reconexion.

**Alternatives considered**:

- Solo deltas despues del REST inicial: descartado porque puede perder cambios que ocurren entre el
  REST y la suscripcion.
- Polling periodico: descartado por peor experiencia y mayor carga.
