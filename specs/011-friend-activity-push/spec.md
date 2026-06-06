# Feature Specification: Actividad en vivo de amigos

**Feature Branch**: `012-friend-activity-push`

**Created**: 2026-06-06

**Status**: Draft

**Input**: User description: "Agregar estado en tiempo real de si un amigo esta jugando despues del
GET de amistades, con actualizaciones por WebSocket para mostrar disponibilidad de spectate"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver amigos jugando en tiempo real (Priority: P1)

Como jugador autenticado que abre su lista de amigos, quiero ver si cada amigo esta jugando una
partida espectable y que ese estado se mantenga actualizado sin refrescar manualmente, para entrar a
espectar cuando la partida este disponible.

**Why this priority**: Es el valor central de la feature: la lista de amigos deja de ser un snapshot
estatico y se convierte en una vista confiable para descubrir partidas espectables.

**Independent Test**: Puede probarse con dos amigos confirmados: uno abre la lista de amigos, el
otro
inicia y termina una partida, y la lista refleja ambos cambios sin recargar la pantalla.

**Acceptance Scenarios**:

1. **Given** un jugador autenticado con un amigo confirmado que no esta jugando, **When** el amigo
   inicia una partida espectable, **Then** la lista muestra que ese amigo esta jugando y habilita la
   accion de espectar.
2. **Given** un jugador autenticado con un amigo confirmado que esta jugando, **When** esa partida
   deja de estar disponible para spectate, **Then** la lista deja de mostrarla como espectable sin
   requerir refresco manual.

---

### User Story 2 - Mantener consistencia entre carga inicial y cambios posteriores (Priority: P2)

Como jugador que entra a la seccion social, quiero que el estado inicial de mis amigos y las
actualizaciones posteriores no se contradigan, para no ver acciones de spectate obsoletas o
faltantes.

**Why this priority**: Evita una experiencia inconsistente cuando un amigo empieza o termina una
partida alrededor del momento en que el usuario abre la lista.

**Independent Test**: Puede probarse abriendo la lista mientras un amigo cambia de estado y
verificando que el resultado final refleje el estado real mas reciente.

**Acceptance Scenarios**:

1. **Given** un jugador carga la lista de amigos mientras un amigo inicia una partida, **When** se
   completa la carga inicial y llegan las novedades pendientes, **Then** el amigo queda representado
   una sola vez con el estado mas reciente.
2. **Given** un jugador carga la lista de amigos mientras un amigo termina una partida, **When** se
   completa la carga inicial y llegan las novedades pendientes, **Then** la accion de espectar no
   queda disponible para una partida finalizada.

---

### User Story 3 - Limitar novedades a amistades vigentes (Priority: P3)

Como jugador, quiero recibir cambios de actividad solo de mis amistades confirmadas vigentes, para
no
ver actividad de usuarios con los que ya no tengo relacion social aceptada.

**Why this priority**: Mantiene la privacidad social y evita mostrar disponibilidad de spectate
fuera
del limite de amistad confirmada.

**Independent Test**: Puede probarse eliminando una amistad mientras el usuario tiene la lista
abierta
y verificando que el amigo deje de actualizarse como disponible.

**Acceptance Scenarios**:

1. **Given** un jugador tiene abierta la lista de amigos, **When** una amistad confirmada se
   elimina,
   **Then** el amigo deja de aparecer como amistad activa y no se muestran nuevas partidas
   espectables para ese usuario.
2. **Given** un jugador recibe una solicitud de amistad aun no aceptada, **When** el otro usuario
   juega una partida, **Then** no se muestra su actividad como partida espectable.

### Edge Cases

- Si un amigo inicia y termina una partida rapidamente, la lista debe converger al estado final mas
  reciente conocido.
- Si un amigo empieza una partida que no es espectable para el jugador, la lista no debe mostrar una
  accion de spectate disponible.
- Si el jugador pierde temporalmente la conexion en vivo, la lista debe poder recuperar un estado
  correcto al reconectar o refrescar la vista social.
- Si una amistad se elimina mientras una partida aparece como espectable, la disponibilidad debe
  desaparecer para ambos usuarios afectados.
- Si el jugador ya esta espectando otra partida, la lista puede seguir mostrando amigos jugando,
  pero
  no debe prometer que podra entrar a otra partida simultaneamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE incluir en la lista inicial de amigos confirmados la informacion de si
  cada amigo tiene una partida actualmente espectable para el jugador autenticado.
- **FR-002**: El sistema DEBE notificar al jugador autenticado cuando un amigo confirmado comienza a
  tener una partida espectable.
- **FR-003**: El sistema DEBE notificar al jugador autenticado cuando la partida espectable de un
  amigo confirmado deja de estar disponible.
- **FR-004**: El sistema DEBE representar la actividad de cada amigo con un estado nullable: sin
  partida espectable o con una referencia minima a la partida espectable.
- **FR-005**: La referencia minima de partida espectable DEBE permitir que el jugador inicie el
  flujo
  existente de spectate sin exponer informacion privada de la partida.
- **FR-006**: El sistema DEBE limitar las actualizaciones de actividad a amistades aceptadas y
  vigentes del jugador autenticado.
- **FR-007**: El sistema DEBE dejar de enviar o mostrar actividad de un usuario cuando la amistad
  con
  ese usuario deja de estar aceptada.
- **FR-008**: El sistema DEBE resolver cambios concurrentes entre la carga inicial y las novedades
  en
  vivo de forma que la vista final refleje el estado mas reciente conocido.
- **FR-009**: El sistema DEBE permitir que el cliente reconstruya un estado correcto de actividad de
  amigos despues de una reconexion o refresco de la vista social.
- **FR-010**: La informacion de actividad de amigos NO DEBE incluir cartas, acciones disponibles,
  estado privado por asiento ni datos de ronda que solo correspondan al flujo de espectador.

### Key Entities

- **Friend Activity**: Estado visible de un amigo confirmado dentro de la lista social. Indica si el
  amigo no tiene partida espectable o si existe una partida que el jugador puede intentar espectar.
- **Spectatable Match Reference**: Referencia minima de una partida disponible para spectate.
  Incluye
  solo los datos necesarios para iniciar la accion de mirar, sin contenido privado del match.
- **Friend Activity Change**: Novedad de actividad asociada a un amigo confirmado. Puede publicar
  una
  nueva referencia espectable o remover la referencia existente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 95% de los cambios de actividad de amigos visibles se reflejan en la lista social
  en
  menos de 2 segundos bajo condiciones normales de conexion.
- **SC-002**: Un jugador puede abrir la lista de amigos y ver el estado inicial de actividad de
  hasta
  100 amigos en menos de 3 segundos.
- **SC-003**: En pruebas de aceptacion, el 100% de las partidas finalizadas o no espectables dejan
  de
  ofrecer la accion de spectate tras recibir la actualizacion correspondiente.
- **SC-004**: En pruebas de privacidad, 0 eventos de actividad incluyen cartas, acciones privadas o
  estado oculto de la partida.
- **SC-005**: En escenarios de reconexion, la vista de amigos converge a un estado correcto sin
  duplicar amigos ni mantener partidas obsoletas.

## Assumptions

- La feature se aplica solo a usuarios autenticados con amistades aceptadas.
- La lista social existente sigue siendo la fuente de carga inicial de amigos y de su estado
  visible.
- "Amigo jugando" significa que existe una partida actualmente espectable para el usuario, no
  cualquier ocupacion interna del amigo.
- Si un amigo participa en mas de una actividad, la vista social muestra como maximo una partida
  espectable activa por amigo.
- La accion de entrar como espectador sigue siendo un flujo separado de la presencia social.
- La modalidad de juego mantiene las reglas propias de truco-to-three: partida a exactamente 3
  puntos, pasarse de 3 pierde, y series mejor de 1, 3 o 5 cuando aplique.
