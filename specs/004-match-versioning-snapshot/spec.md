# Feature Specification: Versionado de Match para Reconciliación Snapshot + Stream

**Feature Branch**: `004-match-versioning-snapshot`

**Created**: 2026-05-25

**Status**: Draft

**Input**: User description: "Resolver el problema de reconciliación entre el snapshot del match (
GET) y el stream de eventos vía WebSocket, evitando duplicaciones, eventos perdidos y
desincronización tras conexión inicial o reconexión. Adoptar la estrategia A: una única secuencia de
`version` global por match, donde todo evento que represente una transición del aggregate `Match` se
emite a todos los jugadores (con payload eventualmente redactado para preservar información oculta),
y donde las notificaciones que no cambian el aggregate no consumen `version`."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Conexión inicial al match sin perder ni duplicar eventos (Priority: P1)

Un jugador acaba de crear o unirse a un match. Entre que recibe el `matchId` (vía respuesta de la
creación/unión) y logra suscribirse al WebSocket usando ese id, pueden ocurrir eventos en el match (
por ejemplo, repartido de cartas o un canto rápido de un bot). El jugador necesita poder reconstruir
la pantalla con un snapshot del estado actual y, a la vez, garantizar que todo evento posterior se
aplica sin duplicar lo que ya estaba en el snapshot ni perder lo que ocurrió mientras se conectaba.

**Why this priority**: Es la causa raíz del problema reportado. Sin esto, el frontend pinta estados
incorrectos en cada arranque de match. Bloquea cualquier UX confiable de juego en vivo.

**Independent Test**: Crear un match, demorar artificialmente la suscripción al WebSocket, forzar la
emisión de varios eventos en ese intervalo, ejecutar luego el GET del estado y abrir el socket.
Verificar que la vista resultante refleja exactamente el estado real del match, sin eventos
duplicados ni omitidos.

**Acceptance Scenarios**:

1. **Given** un match recién creado en el que ocurrieron N eventos antes de que el cliente se
   suscriba al socket, **When** el cliente solicita el snapshot del match y luego comienza a recibir
   eventos, **Then** el cliente puede determinar de forma inequívoca qué eventos del stream ya están
   incluidos en el snapshot y cuáles debe aplicar a continuación.
2. **Given** un cliente que se suscribió al socket antes de emitir el GET de snapshot, **When**
   llegan eventos por el stream mientras el GET está en vuelo, **Then** ningún evento se aplica dos
   veces ni se pierde al combinar snapshot y stream.
3. **Given** un cliente que recibió el snapshot del match, **When** procesa el stream de eventos en
   vivo posterior, **Then** los números de transición del match avanzan de forma estrictamente
   consecutiva y sin huecos.

---

### User Story 2 - Reconexión tras pérdida de socket sin desincronizarse (Priority: P1)

Un jugador estaba conectado a un match y pierde la conexión (cambio de red, ventana en background,
corte momentáneo). Cuando vuelve, debe poder retomar la vista del match en su estado correcto, sin
importar cuántos eventos ocurrieron durante la desconexión, y debe darse cuenta si su stream local
quedó desactualizado para reconciliarse contra el servidor.

**Why this priority**: La reconexión es tan crítica como la conexión inicial: una sesión móvil puede
reconectar varias veces durante un mismo match. Sin un mecanismo de versionado, el cliente no puede
distinguir entre "todo al día" y "perdí cosas".

**Independent Test**: Simular una pérdida y restauración del socket durante un match en curso.
Verificar que el cliente puede detectar que perdió eventos, recuperar el estado correcto mediante el
snapshot, y continuar aplicando eventos en vivo sin duplicaciones ni huecos.

**Acceptance Scenarios**:

1. **Given** un cliente que perdió el socket y al reconectar recibe un evento cuyo número de
   transición no es el siguiente al último que tenía aplicado, **When** el cliente detecta el hueco,
   **Then** dispone de información suficiente para decidir resincronizar contra el snapshot del
   servidor.
2. **Given** un cliente que reconectó y solicitó nuevamente el snapshot, **When** lo aplica y luego
   procesa los eventos en vivo, **Then** el estado final coincide con el del servidor y no se aplica
   ningún evento ya incluido en el snapshot.

---

### User Story 3 - Eventos con información oculta sin romper la secuencia global (Priority: P1)

En el juego existen eventos cuyo contenido es distinto para cada jugador (típicamente, el reparto de
cartas: cada uno ve sólo su mano). Estos eventos siguen siendo transiciones del estado del match (
cambian la ronda, fase, etc.), por lo que todos los jugadores deben avanzar el mismo número de
transición al recibirlos, aunque vean payloads distintos.

**Why this priority**: Sin esta regla, los jugadores tendrían distintos contadores y dejarían de
poder usar la secuencia global para detectar huecos. La estrategia A depende de que toda transición
de estado sea visible (aunque con payload redactado) para todos los jugadores del match.

**Independent Test**: Provocar un evento de información oculta (por ejemplo, un reparto). Verificar
que todos los jugadores del match reciben un evento con el mismo número de transición y mismo tipo,
pero con la porción privada del payload filtrada a lo que cada uno está autorizado a ver.

**Acceptance Scenarios**:

1. **Given** un match donde se produce una transición que contiene información privada para un
   subconjunto de jugadores, **When** se emite el evento por el stream, **Then** todos los jugadores
   del match reciben una versión del evento con el mismo número de transición y tipo, y el payload
   contiene únicamente la información que cada destinatario está autorizado a ver.
2. **Given** dos jugadores del mismo match, **When** ambos consultan el snapshot, **Then** cada uno
   recibe sólo la información que le corresponde, pero ambos snapshots reportan el mismo número de
   transición actual del match.

---

### User Story 4 - Notificaciones no transicionales no consumen número de transición (Priority: P2)

Existen mensajes dirigidos a un jugador que no representan un cambio de estado del match (por
ejemplo, un aviso de "es tu turno" derivado del estado actual, o notificaciones de chat). Estos no
deben hacer avanzar la secuencia global porque no son transiciones del aggregate; si lo hicieran,
distintos jugadores verían huecos artificiales al no recibirlos todos.

**Why this priority**: Es una regla de higiene de la estrategia A. No bloquea las historias
anteriores, pero es necesaria para que la detección de huecos siga siendo confiable en presencia de
notificaciones por jugador.

**Independent Test**: Generar una notificación dirigida a un solo jugador que no cambie el estado
del match. Verificar que dicho mensaje no avanza el número de transición y que el resto de los
jugadores no perciben un hueco en su secuencia.

**Acceptance Scenarios**:

1. **Given** una notificación dirigida a un único jugador que no altera el estado del match, **When
   ** se entrega al destinatario, **Then** no se incrementa el número de transición del match y los
   demás jugadores no observan saltos en su secuencia.

---

### Edge Cases

- ¿Qué pasa si el cliente se suscribe al socket pero el GET de snapshot falla? El cliente debe poder
  reintentar el snapshot sin haber descartado los eventos que llegaron por el socket en el ínterin.
- ¿Qué pasa si llega un evento por el socket cuyo número de transición es menor o igual al ya
  aplicado por el cliente? Debe poder descartarse de forma segura como duplicado o "ya incluido en
  el snapshot".
- ¿Qué pasa si llega un evento cuyo número de transición salta más de uno respecto del último
  aplicado? El cliente debe poder detectar la situación como pérdida de eventos y resolverla con un
  nuevo snapshot.
- ¿Qué pasa si el servidor emite el mismo evento dos veces ante un error de transporte (reintento)?
  Al traer el mismo número de transición, el cliente lo identifica como duplicado y no lo aplica dos
  veces.
- ¿Qué pasa con jugadores espectadores o que se incorporan a mitad de match (si aplica)? Deben poder
  construir su vista inicial mediante el mismo mecanismo de snapshot + stream y mismo número de
  transición.
- ¿Qué pasa cuando un jugador no autorizado a ver cierta información intenta inferirla a partir del
  payload redactado? El payload redactado no debe filtrar datos privados ni siquiera de forma
  indirecta (por ejemplo, longitudes o presencia de campos sensibles).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST asociar a cada match un número de transición monotónicamente creciente
  que se incrementa en exactamente uno por cada transición de estado del aggregate del match.
- **FR-002**: El snapshot del estado del match expuesto al cliente MUST incluir el número de
  transición actual del match en el momento en que se computa el snapshot.
- **FR-003**: Cada evento emitido por el stream en vivo del match MUST incluir el número de
  transición resultante del match tras aplicar ese evento.
- **FR-004**: Toda transición del estado del match MUST entregarse a todos los jugadores del match a
  través del stream en vivo, sin omitir a ninguno por razones de privacidad de payload.
- **FR-005**: El sistema MUST permitir que el payload de un evento sea distinto por jugador cuando
  el evento contiene información oculta, entregando a cada jugador únicamente la porción que está
  autorizado a ver, manteniendo idénticos el número de transición y el tipo del evento entre todas
  las variantes.
- **FR-006**: Las notificaciones dirigidas a uno o varios jugadores que no representan una
  transición del estado del match MUST NO incrementar el número de transición del match y MUST
  entregarse por un canal distinguible del stream de transiciones (de modo que el cliente no las
  confunda con eventos secuenciados).
- **FR-007**: El cliente MUST poder reconciliar el snapshot inicial con eventos del stream
  descartando, sin aplicarlos, aquellos cuyo número de transición sea menor o igual al número de
  transición del snapshot.
- **FR-008**: El cliente MUST poder detectar un hueco en la secuencia cuando recibe un evento cuyo
  número de transición no es el siguiente esperado, distinguiéndolo del caso "evento ya incluido en
  el snapshot".
- **FR-009**: El sistema MUST preservar la semántica de tipo del evento (qué transición ocurrió)
  además del número de transición, para que el cliente pueda animar o reaccionar al cambio
  específico, no sólo recomputar estado.
- **FR-010**: El número de transición de un match MUST persistir y mantenerse consistente ante
  reinicios del servidor, de forma que un cliente que reconecta no observe retrocesos ni
  reasignaciones del número.
- **FR-011**: La redacción del payload por jugador MUST garantizar que la información oculta no se
  filtra ni siquiera indirectamente (por presencia de campos, longitudes o metadatos derivados).
- **FR-012**: El sistema MUST mantener el contrato existente del snapshot del match agregando el
  número de transición como información adicional, sin romper a clientes que aún no lo consumen.

### Key Entities *(include if feature involves data)*

- **Match (aggregate)**: Representa una partida en curso. Pasa a tener asociado un número de
  transición que refleja cuántas transiciones de estado ha sufrido desde su creación.
- **Match Transition Event (evento de transición)**: Evento de dominio que representa un cambio de
  estado del match (reparto, canto, jugada, resolución, cambio de score, fin de mano, fin de match,
  etc.). Es lo que avanza el número de transición.
- **Match Snapshot (snapshot)**: Vista completa del estado actual del match para un jugador o
  espectador concreto. Acompañado del número de transición vigente al momento de generarse.
- **Player-Scoped Event Payload (payload redactado)**: Variante del payload de un evento de
  transición filtrada según lo que un jugador específico está autorizado a ver. Comparte número de
  transición y tipo con las otras variantes del mismo evento.
- **Player Notification (notificación por jugador)**: Mensaje dirigido a uno o varios jugadores que
  no representa una transición del aggregate y por tanto no consume número de transición.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de las conexiones iniciales a un match recién creado, el cliente puede
  reconstruir el estado correcto del match sin duplicar ni omitir eventos, incluso si ocurrieron
  transiciones entre la creación del match y la suscripción al stream.
- **SC-002**: En el 100% de los escenarios de reconexión durante un match en curso, el cliente puede
  detectar si perdió eventos y, en caso afirmativo, recuperar el estado correcto del servidor sin
  intervención del usuario.
- **SC-003**: Para cualquier match con eventos de información oculta, todos los jugadores del match
  observan exactamente la misma cantidad de transiciones aplicadas (mismo número de transición
  vigente) ante los mismos hechos del juego, aun cuando vean payloads distintos.
- **SC-004**: Cero eventos duplicados aplicados por el cliente en pruebas de estrés que combinan
  emisiones repetidas, suscripción tardía y reconexión durante una sesión de juego.
- **SC-005**: Cero huecos no detectados en la secuencia de transiciones del match: si se pierde un
  evento por cualquier causa, el cliente lo detecta antes de pintar estado incorrecto.

## Assumptions

- La estrategia adoptada es "A": una única secuencia global de número de transición por match; las
  notificaciones que no son transiciones del aggregate viajan por un canal aparte sin consumir
  secuencia.
- El stream de eventos en vivo del match continúa entregándose a cada jugador por su propio canal (
  no se asume un canal público compartido para el match), pero el contenido transicional debe ser
  equivalente entre jugadores salvo por la redacción de información oculta.
- El snapshot del match seguirá obteniéndose mediante una operación de lectura puntual contra el
  backend (modelo snapshot + stream); este feature no introduce un mecanismo alternativo de replay
  completo de eventos desde origen.
- Los clientes existentes que no consuman el número de transición seguirán funcionando con el
  comportamiento previo; el feature añade información, no la reemplaza.
- La autorización sobre qué información puede ver cada jugador se rige por las reglas del dominio ya
  existentes (mano del jugador, espectadores, etc.); este feature no redefine esas reglas, sólo
  asegura que la redacción del payload las respete.
- El alcance se limita al bounded context de Match. Otros contextos (liga, copa, rematch, chat
  global) podrán adoptar el mismo patrón en el futuro pero no forman parte de este feature.
