# Feature Specification: Disponibilidad de amigos para invitaciones

**Feature Branch**: `013-friend-availability`

**Created**: 2026-06-06

**Status**: Draft

**Input**: User description: "Extender la actividad social de amigos para mostrar si un amigo esta
disponible para ser invitado a un match. Un amigo debe figurar ocupado por cualquier estado que
bloquee recibir o aceptar una invitacion, incluyendo solicitudes o invitaciones pendientes solo si
realmente bloquean. Agregar tambien si esta online, como dato separado."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Saber si puedo invitar a un amigo (Priority: P1)

Como jugador autenticado que ve su lista de amigos, quiero saber si cada amigo puede recibir o
aceptar una invitacion a una partida en este momento, para no intentar invitar a alguien que el
sistema rechazaria como ocupado.

**Why this priority**: Es el valor principal de la feature: evita acciones fallidas y hace que la
lista social refleje la disponibilidad real para jugar con amigos.

**Independent Test**: Puede probarse con dos amigos confirmados: uno queda libre, luego entra en
cada estado bloqueante conocido, y la lista cambia entre disponible y ocupado segun corresponda.

**Acceptance Scenarios**:

1. **Given** un jugador autenticado con un amigo confirmado sin estados bloqueantes, **When** abre
   la
   lista social, **Then** el amigo aparece como disponible para invitar.
2. **Given** un amigo confirmado tiene un estado que bloquearia recibir o aceptar una invitacion,
   **When** el jugador ve la lista social, **Then** el amigo aparece como ocupado y se informa el
   motivo de ocupacion.
3. **Given** un amigo confirmado deja de tener estados bloqueantes, **When** la lista social recibe
   la actualizacion correspondiente, **Then** el amigo vuelve a aparecer como disponible.

---

### User Story 2 - Ver cambios de disponibilidad en vivo (Priority: P2)

Como jugador con la lista social abierta, quiero que la disponibilidad de mis amigos cambie en vivo
cuando entran o salen de estados bloqueantes, para decidir a quien invitar sin refrescar la
pantalla.

**Why this priority**: Mantiene la lista social sincronizada durante el uso normal y reduce la
friccion de invitar a amigos.

**Independent Test**: Puede probarse manteniendo abierta la lista mientras un amigo entra a una
partida, una cola, un torneo o una revancha abierta, y verificando que el estado cambia sin
recargar.

**Acceptance Scenarios**:

1. **Given** un jugador ve un amigo disponible, **When** ese amigo entra a un estado bloqueante,
   **Then** la lista lo marca como ocupado sin refresco manual.
2. **Given** un jugador ve un amigo ocupado, **When** ese amigo se libera, **Then** la lista lo
   marca
   como disponible sin refresco manual.
3. **Given** un amigo cambia varias veces de estado mientras el jugador esta conectado, **When** se
   aplican las actualizaciones, **Then** la lista converge al estado mas reciente conocido.

---

### User Story 3 - Distinguir online de disponible (Priority: P3)

Como jugador, quiero saber si un amigo esta online aunque no pueda invitarlo, para entender si esta
conectado al juego sin confundir conexion con disponibilidad para jugar.

**Why this priority**: La presencia online agrega contexto social, pero no debe mezclarse con la
regla de negocio que habilita o bloquea invitaciones.

**Independent Test**: Puede probarse conectando y desconectando sesiones de un amigo y verificando
que `online` cambia sin alterar por si solo la disponibilidad.

**Acceptance Scenarios**:

1. **Given** un amigo esta conectado y libre, **When** el jugador abre la lista social, **Then** el
   amigo aparece online y disponible.
2. **Given** un amigo esta conectado pero ocupado, **When** el jugador abre la lista social, **Then
   **
   el amigo aparece online y ocupado.
3. **Given** un amigo no tiene sesiones activas pero no posee estados bloqueantes, **When** el
   jugador abre la lista social, **Then** el amigo aparece offline pero disponible segun reglas de
   invitacion.

---

### User Story 4 - Respetar privacidad y amistades vigentes (Priority: P4)

Como jugador, quiero recibir disponibilidad solo de amistades aceptadas vigentes, para que mi estado
social no se exponga a usuarios que no son mis amigos.

**Why this priority**: La disponibilidad y el estado online son datos sociales sensibles y deben
respetar los limites de amistad confirmada.

**Independent Test**: Puede probarse eliminando una amistad y verificando que ambas partes dejan de
recibir disponibilidad, presencia y actividad del otro.

**Acceptance Scenarios**:

1. **Given** dos usuarios ya no son amigos aceptados, **When** uno cambia de disponibilidad u
   online, **Then** el otro no recibe ni ve ese cambio.
2. **Given** existe una solicitud de amistad pendiente, **When** cualquiera de los dos cambia de
   disponibilidad u online, **Then** el otro no recibe estado social hasta que la amistad sea
   aceptada.

### Edge Cases

- Si un amigo tiene mas de un estado bloqueante al mismo tiempo, la lista debe mostrar un unico
  motivo principal estable y comprensible.
- Si una solicitud o invitacion pendiente no bloquea crear, recibir ni aceptar invitaciones, no debe
  marcar al amigo como ocupado.
- Si una invitacion pendiente si bloquea nuevas invitaciones para el mismo amigo y recurso, debe
  mostrarse como motivo de ocupacion solo en ese alcance.
- Si un amigo tiene varias sesiones abiertas, debe aparecer online mientras al menos una sesion siga
  activa.
- Si una desconexion no se detecta inmediatamente, `online` puede ser aproximado y debe corregirse
  cuando expire la sesion o se reconcilie el estado.
- Si el jugador pierde temporalmente la conexion en vivo, la lista debe poder reconstruir un estado
  correcto al reconectar o recargar la vista social.
- Si un amigo esta en una partida espectable, la lista puede ofrecer la accion de mirar, pero esa
  accion no reemplaza ni modifica la disponibilidad para invitar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE incluir en la lista inicial de amigos aceptados el estado de
  disponibilidad de cada amigo para recibir o aceptar una invitacion a partida.
- **FR-002**: El sistema DEBE marcar a un amigo como `AVAILABLE` cuando ninguna regla vigente
  bloquearia recibir o aceptar una invitacion a partida.
- **FR-003**: El sistema DEBE marcar a un amigo como `BUSY` cuando cualquier regla vigente
  bloquearia recibir o aceptar una invitacion a partida.
- **FR-004**: El sistema DEBE informar un `busyReason` cuando el amigo este `BUSY`, usando motivos
  de negocio comprensibles como partida activa, cola de partida rapida, liga, copa, revancha abierta
  o invitacion/solicitud pendiente solo cuando ese pendiente realmente bloquee la invitacion.
- **FR-005**: El sistema NO DEBE marcar como ocupado a un amigo por una solicitud o invitacion
  pendiente que no bloquee crear, recibir ni aceptar una invitacion segun las reglas vigentes.
- **FR-006**: El sistema DEBE incluir un indicador `online` separado de `availability`, basado en si
  el amigo tiene al menos una sesion activa conocida.
- **FR-007**: El indicador `online` NO DEBE cambiar por si mismo la disponibilidad para invitar; un
  amigo puede estar offline y disponible, u online y ocupado.
- **FR-008**: El sistema DEBE notificar al jugador cuando cambie la disponibilidad, el motivo de
  ocupacion, la presencia online o la partida espectable de un amigo aceptado.
- **FR-009**: El sistema DEBE permitir reconstruir el estado completo de disponibilidad de amigos
  despues de una reconexion o recarga de la vista social.
- **FR-010**: El sistema DEBE limitar la visibilidad de disponibilidad, online y partida espectable
  a amistades aceptadas y vigentes.
- **FR-011**: El sistema DEBE dejar de mostrar y enviar disponibilidad de un usuario cuando la
  amistad con ese usuario deja de estar aceptada.
- **FR-012**: Cuando un amigo tenga una partida actualmente espectable, el sistema DEBE incluir una
  referencia minima que permita iniciar el flujo existente de espectador.
- **FR-013**: La informacion social de disponibilidad NO DEBE incluir cartas, acciones disponibles,
  estado privado por asiento, datos privados de ronda ni datos internos innecesarios para decidir si
  invitar o mirar.
- **FR-014**: Si existen varios motivos de ocupacion simultaneos, el sistema DEBE elegir un motivo
  principal deterministico para que el cliente no muestre estados contradictorios.

### Key Entities

- **Friend Availability**: Estado visible de un amigo aceptado desde la perspectiva del jugador.
  Atributos principales: username del amigo, `online`, `availability`, `busyReason` opcional y
  partida espectable opcional.
- **Availability Status**: Resultado de negocio que indica si el amigo puede ser invitado
  (`AVAILABLE`) o si una regla vigente lo bloquea (`BUSY`).
- **Busy Reason**: Motivo principal por el cual el amigo no puede ser invitado. Debe derivar de una
  regla que realmente bloquee invitaciones.
- **Online Presence**: Indicador aproximado de conexion actual del amigo. Representa sesiones
  activas conocidas y se corrige ante desconexion, expiracion o reconciliacion.
- **Spectatable Match Reference**: Referencia minima de una partida que el jugador puede intentar
  mirar como espectador, sin exponer informacion privada del match.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En pruebas de aceptacion, el 100% de los estados que bloquean invitaciones aparecen
  como `BUSY` con un motivo de ocupacion.
- **SC-002**: En pruebas de aceptacion, el 100% de las solicitudes o invitaciones pendientes que no
  bloquean invitaciones no marcan al amigo como ocupado.
- **SC-003**: El 95% de los cambios visibles de disponibilidad u online se reflejan en la lista
  social en menos de 2 segundos bajo condiciones normales de conexion.
- **SC-004**: Un jugador puede abrir la lista social y ver disponibilidad, online y partida
  espectable de hasta 100 amigos en menos de 3 segundos.
- **SC-005**: En escenarios de reconexion, la vista social converge a un estado correcto sin
  duplicar amigos ni mantener motivos de ocupacion obsoletos.
- **SC-006**: En pruebas de privacidad, 0 eventos sociales de disponibilidad se entregan a usuarios
  que no tienen una amistad aceptada vigente.
- **SC-007**: En pruebas de privacidad, 0 payloads de disponibilidad incluyen cartas, acciones
  privadas o estado oculto de partida.

## Assumptions

- La feature se aplica solo a usuarios autenticados y a amistades aceptadas.
- "Disponible" significa "el sistema permitiria invitarlo o que acepte una invitacion a partida en
  este momento", no necesariamente que este conectado.
- La presencia online es aproximada y puede corregirse por reconciliacion tras desconexiones,
  expiraciones o multiples sesiones.
- Las reglas que bloquean invitaciones son las reglas vigentes del producto; si una nueva regla de
  bloqueo se agrega en el futuro, debe reflejarse en `availability`.
- Una solicitud o invitacion pendiente solo se considera bloqueo si la regla de negocio actual la
  usa para rechazar nuevas invitaciones o aceptaciones.
- La accion de entrar como espectador sigue siendo un flujo separado de la disponibilidad social.
- La modalidad de juego mantiene las reglas propias de truco-to-three: partida a exactamente 3
  puntos, pasarse de 3 pierde, y series mejor de 1, 3 o 5 cuando aplique.
