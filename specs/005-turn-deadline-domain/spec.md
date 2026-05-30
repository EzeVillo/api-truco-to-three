# Feature Specification: Deadline de turno como concepto de dominio

**Feature Branch**: `005-turn-deadline-domain`

**Created**: 2026-05-28

**Status**: Draft

**Input**: User description: "avancemos con esto entonces, te parece?" (continuación de la
discusión sobre exponer el temporizador de turno por inactividad, definiendo el deadline como
responsabilidad del dominio en lugar de la capa de aplicación)

## Contexto

Hoy una partida en curso fuerza un forfeit administrativo cuando el jugador que debe actuar agota su
plazo. Ese plazo existe, se cumple y decide partidas (impacta liga, copa y logros), pero **no se le
muestra al jugador ni al espectador**: el reloj vive solo como un agendamiento técnico de la capa de
aplicación y no hay un valor canónico expuesto.

Esta feature persigue dos objetivos de negocio:

1. **Mostrar la cuenta regresiva** del turno a jugadores y espectadores, sobre el asiento que debe
   actuar.
2. **Garantizar que lo mostrado es exactamente lo que se ejecuta**: un único deadline autoritativo,
   sin divergencia entre el reloj visible y el forfeit real.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El jugador ve cuánto tiempo le queda para actuar (Priority: P1)

Durante una partida en curso, el jugador que debe jugar una carta o responder un canto ve una cuenta
regresiva que indica cuánto tiempo le resta antes de perder por inactividad. El rival también ve esa
misma cuenta regresiva sobre el asiento que debe actuar. El tiempo mostrado coincide con el momento
exacto en que el sistema declarará el forfeit.

**Why this priority**: Es el corazón de la feature y el mínimo viable. Sin un reloj visible y
fidedigno, el jugador puede perder una partida sin entender por qué; con un reloj que no coincide
con
el forfeit real, la experiencia es peor que no tener reloj (forfeits "fantasma" o sorpresivos).

**Independent Test**: Iniciar una partida, observar que el asiento que debe actuar expone instante
de
vencimiento, duración total y asiento; dejar correr el plazo y verificar que el forfeit ocurre en el
mismo instante que mostraba la cuenta regresiva.

**Acceptance Scenarios**:

1. **Given** una partida en curso donde es el turno de un jugador, **When** el jugador consulta el
   estado de la partida, **Then** recibe el instante de vencimiento, la duración total del turno y
   el
   asiento que debe actuar.
2. **Given** una partida en curso, **When** el asiento que debe actuar cambia (cambio de turno,
   canto
   de truco/envido, respuesta que devuelve el juego al rival, o nueva ronda), **Then** la cuenta
   regresiva se reinicia con un nuevo vencimiento sobre el nuevo asiento.
3. **Given** un canto pendiente de respuesta, **When** se observa el temporizador, **Then** el reloj
   aplica sobre el asiento que debe responder, que puede no ser el del turno de juego.
4. **Given** una partida en curso, **When** el jugador que debe actuar agota su plazo, **Then** el
   sistema declara el forfeit en el mismo instante que indicaba la cuenta regresiva.

---

### User Story 2 - El espectador ve la misma cuenta regresiva (Priority: P2)

Un espectador habilitado de una partida ve la misma cuenta regresiva que los jugadores, renderizada
sobre el asiento que debe actuar, sin acceder a información privada de los asientos (cartas o
acciones
disponibles).

**Why this priority**: Completa la experiencia de espectador (liga/copa) y reutiliza el mismo valor
autoritativo. Depende de que P1 ya defina el deadline, pero aporta valor independiente.

**Independent Test**: Registrarse como espectador de una partida en curso y verificar que el
snapshot
y las actualizaciones en vivo incluyen el vencimiento, la duración y el asiento, sin exponer cartas
ni acciones privadas.

**Acceptance Scenarios**:

1. **Given** un espectador registrado de una partida en curso, **When** consulta el estado de
   espectador, **Then** recibe el instante de vencimiento, la duración y el asiento que debe actuar.
2. **Given** un espectador conectado en vivo, **When** cambia el asiento que debe actuar, **Then**
   recibe la actualización del nuevo plazo igual que los jugadores.

---

### User Story 3 - La cuenta regresiva sigue siendo fiel tras reconexión o reinicio (Priority: P3)

Si un jugador o espectador se reconecta —o si el servicio se reinicia— la cuenta regresiva que ve
refleja el tiempo restante real del turno en curso, no un plazo reiniciado desde cero.

**Why this priority**: Evita que una reconexión "regale" tiempo (o lo quite) respecto del plazo real
que el sistema va a ejecutar. Es refinamiento de consistencia sobre P1, no MVP.

**Independent Test**: Con un turno en curso, simular reconexión del cliente (y, por separado, un
reinicio del servicio) y verificar que el vencimiento expuesto sigue apuntando al mismo instante
absoluto que antes.

**Acceptance Scenarios**:

1. **Given** un turno con cuenta regresiva en progreso, **When** el cliente se reconecta y vuelve a
   leer el estado, **Then** el vencimiento expuesto apunta al mismo instante absoluto que antes de
   desconectarse.
2. **Given** un turno con cuenta regresiva en progreso, **When** el servicio se reinicia, **Then**
   el
   plazo que se ejecuta y el que se expone siguen apuntando al mismo instante de vencimiento.

---

### Edge Cases

- **Sin asiento que deba actuar**: cuando una mano se está resolviendo o el juego está en un estado
  donde ningún asiento debe actuar, el sistema debe señalar explícitamente que el reloj no corre (no
  mostrar una cuenta regresiva falsa).
- **Vencimiento ya pasado al leer el snapshot**: si el cliente lee el estado en el instante del
  vencimiento o después, debe poder representar "sin tiempo restante" de forma coherente con el
  forfeit que está por ocurrir.
- **Desfase de reloj cliente↔servidor**: el cliente debe poder neutralizar la diferencia de reloj a
  partir de una referencia temporal del servidor para que el restante mostrado sea correcto.
- **Asiento que debe actuar distinto del turno de juego**: ante un canto pendiente, el reloj y el
  asiento expuesto deben corresponder al que debe responder, no al del turno.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer, para toda partida en curso, tres datos del temporizador del
  turno: el instante de vencimiento (en qué momento el asiento que debe actuar pierde por
  inactividad), la duración total del plazo y el asiento que debe actuar.
- **FR-002**: El sistema MUST exponer esos tres datos tanto en el estado completo de la partida
  (snapshot consultable) como en las actualizaciones en vivo, para jugadores y espectadores.
- **FR-003**: El instante de vencimiento expuesto MUST ser el mismo que el sistema utiliza para
  declarar el forfeit. No puede existir un valor "mostrado" distinto del valor "ejecutado".
- **FR-004**: El sistema MUST reiniciar el plazo cada vez que cambia el asiento que debe actuar:
  cambio de turno, canto de truco o envido, respuesta que devuelve el juego al rival, e inicio de
  nueva ronda.
- **FR-005**: El asiento que debe actuar MAY diferir del asiento cuyo turno de juego es, cuando hay
  un
  canto pendiente de respuesta; el temporizador MUST corresponder al asiento que debe responder.
- **FR-006**: Cuando ningún asiento debe actuar (mano resolviéndose, esperas del servidor), el
  sistema MUST señalar que el reloj no corre, de forma que los clientes no muestren una cuenta
  regresiva inexistente.
- **FR-007**: El espectador MUST recibir la misma información de temporizador que los jugadores (es
  información pública), pero MUST NOT recibir información privada por asiento (cartas ni acciones
  disponibles).
- **FR-008**: La información de vencimiento expuesta MUST mantenerse fiel a través de reconexiones
  del
  cliente y reinicios del servicio, apuntando siempre al mismo instante absoluto del turno en curso.
- **FR-009**: El sistema MUST seguir declarando el forfeit del asiento inactivo al vencer el plazo
  (comportamiento existente), manteniéndolo coherente con la cuenta regresiva expuesta.
- **FR-010**: La duración del plazo del turno MUST ser configurable a nivel operativo, sin requerir
  cambios funcionales en el resto del sistema.
- **FR-011**: El sistema MUST exponer una referencia temporal del servidor que permita al cliente
  calcular y neutralizar el desfase de reloj cliente↔servidor para representar el tiempo restante.

### Key Entities *(include if feature involves data)*

- **Deadline de turno**: representa el plazo vigente del turno en una partida en curso. Atributos
  conceptuales: instante absoluto de vencimiento, duración total del plazo, y asiento al que aplica.
  Es propiedad de la partida y constituye la única fuente de verdad tanto para lo que se muestra
  como
  para lo que se ejecuta. Existe solo mientras algún asiento debe actuar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El instante de forfeit ejecutado por el sistema coincide con el vencimiento mostrado
  en
  la cuenta regresiva dentro de un margen de 1 segundo (una vez neutralizado el desfase de reloj del
  cliente), en el 100% de los turnos.
- **SC-002**: El 100% de los cambios de asiento que debe actuar (cambio de turno, canto, respuesta,
  nueva ronda) produce una cuenta regresiva reiniciada sobre el asiento correcto.
- **SC-003**: Cero forfeits "fantasma" (el forfeit se dispara mientras la cuenta regresiva mostrada
  aún tenía tiempo) y cero forfeits "silenciosos" (el forfeit ocurre sin que haya existido una
  cuenta
  regresiva visible para ese turno).
- **SC-004**: Tras una reconexión del cliente, el tiempo restante mostrado refleja el restante real
  del turno (no un reinicio del plazo) en el 100% de los casos.
- **SC-005**: Tras un reinicio del servicio con una partida en curso, el plazo ejecutado y el
  expuesto
  apuntan al mismo instante de vencimiento.

## Assumptions

- La duración por defecto del plazo de turno se mantiene en el valor operativo vigente (orden de ~30
  segundos), configurable; esta feature no redefine el valor, solo lo hace autoritativo y visible.
- El mecanismo de cómputo de desfase de reloj en el cliente se basa en referencias temporales del
  servidor ya disponibles en las comunicaciones en tiempo real.
- El mecanismo de ejecución del forfeit por inactividad ya existe; esta feature centraliza el
  deadline
  como fuente de verdad y lo expone, en lugar de calcularlo de forma dispersa.
- La **representación del deadline en el flujo de eventos en tiempo real** —en particular si la
  señal de plazo participa o no del cursor de reconciliación de estado que usa el cliente— es una
  decisión de diseño que se resolverá en la fase de planificación (`/speckit-plan`), porque tiene
  implicancias en el contrato con el frontend. El spec no la prescribe.
- El alcance es la partida (match) individual; liga y copa solo se ven afectadas indirectamente a
  través del resultado del forfeit ya existente.

## Documentación a actualizar

- **`docs/CONTRATOS_API.md`**: la sección §4.18 y los eventos asociados ya fueron redactados de
  forma
  anticipada; al implementar deberá verificarse que el contrato documentado coincida con la decisión
  de representación que se tome en planificación (ver Assumptions). Si esa decisión cambia la
  semántica del cursor de reconciliación, la sección §9.5 debe ajustarse.
- **`README.md`**: evaluar si corresponde mencionar el temporizador de turno visible como capacidad
  del sistema.
