# Feature Specification: Partidas Bot vs Bot Espectables

**Feature Branch**: `016-bot-vs-bot-spectating`

**Created**: 2026-06-16

**Status**: Draft

**Input**: User description: "quiero poder crear partida entre bots y poder espectearlas — el usuario pasa a estar ocupado hasta que termine ese match, no puede crearlo si está ocupado tampoco, y se pueden ver las cartas de ambos bots"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Crear y espectar una partida entre dos bots viendo ambas manos (Priority: P1)

Un usuario autenticado quiere enfrentar a dos bots entre sí y observar la partida como
espectador. Elige los dos bots y el formato de serie (mejor de 1, 3 o 5), crea la partida, y
queda mirándola en tiempo real. A diferencia de un espectado normal, puede ver las cartas
completas de **ambos** bots, porque ningún humano está jugando y no hay integridad competitiva
que proteger.

**Why this priority**: Es el corazón de la feature. Sin la capacidad de crear el enfrentamiento
bot-vs-bot y verlo con visibilidad total de cartas, no hay producto. Entrega valor por sí sola:
permite estudiar el comportamiento de los bots, comparar personalidades y disfrutar la partida.

**Independent Test**: Se puede probar de punta a punta creando una partida entre dos bots
distintos y verificando que (a) la partida arranca y los bots juegan solos hasta una conclusión,
y (b) el espectador recibe en tiempo real el estado con las cartas de ambos bots visibles.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado y disponible, **When** crea una partida eligiendo dos bots
   distintos y un formato de serie válido, **Then** la partida se crea, arranca automáticamente y
   el usuario queda espectándola.
2. **Given** una partida bot-vs-bot en curso que el usuario está espectando, **When** los bots
   juegan sus cartas y cantan envido/truco, **Then** el espectador ve en tiempo real las cartas
   completas de ambas manos, los cantos, el puntaje y el avance de ronda/juego.
3. **Given** dos bots seleccionados que resultan ser el mismo bot, **When** el usuario intenta
   crear la partida, **Then** la creación es rechazada con un mensaje claro.
4. **Given** un identificador de bot que no existe en el catálogo, **When** el usuario intenta
   crear la partida con él, **Then** la creación es rechazada con un mensaje claro.

---

### User Story 2 - El creador queda ocupado mientras dura la partida (Priority: P1)

Mientras la partida bot-vs-bot está en curso, el usuario que la creó figura como **ocupado** (no
disponible) para el resto del sistema, igual que si estuviera jugando una partida. No puede
iniciar otra actividad que requiera disponibilidad hasta que la partida termine.

**Why this priority**: Es una regla explícita del pedido y evita estados inconsistentes (un mismo
usuario "en" varias partidas a la vez, o apareciendo disponible para amigos/desafíos mientras está
absorto en una exhibición). Es tan crítica como la US1 para considerar la feature correcta.

**Independent Test**: Se puede probar verificando que, con una partida bot-vs-bot activa creada por
el usuario, su estado de disponibilidad pasa a "ocupado" y cualquier acción que exija
disponibilidad lo trata como no disponible.

**Acceptance Scenarios**:

1. **Given** un usuario que acaba de crear una partida bot-vs-bot, **When** se consulta su
   disponibilidad, **Then** figura como ocupado durante toda la duración de esa partida.
2. **Given** un usuario con una partida bot-vs-bot en curso, **When** intenta crear otra partida
   bot-vs-bot, **Then** la creación es rechazada por estar ocupado.
3. **Given** un usuario que ya está ocupado en otra actividad (por ejemplo, jugando una partida),
   **When** intenta crear una partida bot-vs-bot, **Then** la creación es rechazada por estar
   ocupado.

---

### User Story 3 - Al terminar la partida, el creador vuelve a estar disponible (Priority: P2)

Cuando la partida bot-vs-bot concluye (la serie queda decidida), el usuario creador se libera
automáticamente y vuelve a estar disponible para crear o sumarse a otras actividades, sin ninguna
acción manual adicional.

**Why this priority**: Cierra el ciclo de ocupación de la US2. Sin liberación automática, el
usuario quedaría atrapado en estado "ocupado". Es importante, pero depende de que US1/US2 existan.

**Independent Test**: Se puede probar dejando que una partida bot-vs-bot llegue a su conclusión y
verificando que el creador vuelve a estar disponible y puede crear una nueva partida.

**Acceptance Scenarios**:

1. **Given** una partida bot-vs-bot que llega a su conclusión (serie decidida), **When** la partida
   termina, **Then** el usuario creador vuelve a estar disponible automáticamente.
2. **Given** un usuario recién liberado tras terminar su partida bot-vs-bot, **When** intenta crear
   una nueva partida bot-vs-bot, **Then** la creación es aceptada.

---

### Edge Cases

- **Usuario ocupado intenta crear**: la creación se rechaza con un mensaje claro indicando que el
  usuario ya está ocupado; no se crea ninguna partida.
- **Mismo bot en ambos asientos**: se rechaza la creación; una partida requiere dos bots distintos.
- **Bot inexistente o no disponible en el catálogo**: se rechaza la creación.
- **El creador pierde conexión o cierra la aplicación mientras la partida corre**: la partida
  continúa jugándose automáticamente hasta su conclusión; el creador permanece ocupado hasta que la
  partida termina y luego se libera. Al reconectar, puede volver a ver el estado actual de la
  partida (si aún está en curso) con las cartas de ambos bots.
- **Final por punto exacto**: si un bot supera los 3 puntos (regla de punto exacto), pierde esa
  partida; la serie avanza o se decide según el formato, y el creador se libera cuando la serie
  termina.
- **Otro usuario intenta espectar la misma partida**: fuera de alcance — la partida bot-vs-bot es
  una exhibición privada de su creador (ver Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir a un usuario autenticado crear una partida entre dos bots,
  eligiendo ambos bots participantes y el formato de serie (mejor de 1, 3 o 5 partidas).
- **FR-002**: El sistema MUST rechazar la creación si los dos bots seleccionados son el mismo bot.
- **FR-003**: El sistema MUST rechazar la creación si alguno de los bots seleccionados no existe o
  no está disponible en el catálogo de bots.
- **FR-004**: El sistema MUST impedir la creación de una partida bot-vs-bot cuando el usuario que la
  solicita ya está ocupado (no disponible) por cualquier otra actividad.
- **FR-005**: Al crear la partida, el sistema MUST marcar al usuario creador como ocupado (no
  disponible) durante toda la duración de esa partida.
- **FR-006**: El sistema MUST iniciar la partida automáticamente y hacer que ambos bots jueguen sus
  turnos sin intervención humana, respetando las reglas del juego (partida a 3 puntos exactos,
  regla de punto exacto y formato de serie elegido).
- **FR-007**: El sistema MUST permitir al usuario creador espectar en tiempo real la partida que
  creó.
- **FR-008**: Durante el espectado de una partida bot-vs-bot, el sistema MUST mostrar al creador las
  cartas completas de **ambos** bots (ambas manos visibles), a diferencia del espectado de partidas
  con jugadores humanos.
- **FR-009**: El sistema MUST actualizar el estado mostrado al creador en tiempo real a medida que
  la partida avanza (cartas jugadas, cantos de envido/truco y sus respuestas, puntaje, número de
  ronda y de juego dentro de la serie).
- **FR-010**: Al finalizar la partida (serie decidida), el sistema MUST liberar al usuario creador,
  dejándolo disponible nuevamente sin acción manual.
- **FR-011**: Una partida bot-vs-bot MUST NOT generar una sesión de revancha.
- **FR-012**: Una partida bot-vs-bot MUST NOT aparecer en el lobby público de partidas.
- **FR-013**: Una partida bot-vs-bot MUST NOT afectar estadísticas de jugador ni logros, dado que
  ningún jugador humano participa de ella.

### Key Entities *(include if feature involves data)*

- **Partida Bot vs Bot**: una partida cuyos dos asientos están ocupados por bots, asociada al
  usuario que la creó, con un formato de serie (mejor de 1, 3 o 5). Tiene un estado de avance
  (cartas en mesa, manos de ambos bots, puntaje, ronda y juego) y un ciclo de vida que termina
  cuando la serie queda decidida.
- **Creador / Espectador**: el usuario autenticado que crea la partida y la observa. Queda ocupado
  (no disponible) mientras la partida está en curso y se libera al terminar. Es el destinatario de
  la vista con cartas completas de ambos bots.
- **Bot**: jugador virtual existente del catálogo, con una personalidad que determina sus
  decisiones. Dos bots distintos ocupan los asientos de la partida.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario disponible puede crear una partida bot-vs-bot y empezar a verla en una sola
  acción, sin pasos intermedios manuales.
- **SC-002**: El creador ve las cartas completas de ambos bots en el 100% de las manos jugadas
  durante la partida.
- **SC-003**: Mientras su partida bot-vs-bot está en curso, el 100% de los intentos del creador de
  crear otra partida o de ser sumado a una actividad que requiere disponibilidad son rechazados.
- **SC-004**: El 100% de los intentos de crear una partida bot-vs-bot estando ya ocupado son
  rechazados con un mensaje claro y sin crear ninguna partida.
- **SC-005**: Al terminar la partida, el creador queda disponible automáticamente y puede crear una
  nueva partida sin ninguna acción de "liberación" manual.
- **SC-006**: El 100% de las partidas bot-vs-bot creadas se juegan hasta una conclusión (serie
  decidida) sin intervención humana.

## Assumptions

- **Espectador único**: el creador es el único espectador previsto de la partida bot-vs-bot;
  habilitar espectadores adicionales queda fuera del alcance de esta feature. La partida se descubre
  por su identificador directo, no se publica en ningún listado.
- **Visibilidad total justificada**: mostrar las cartas de ambos bots es aceptable porque ningún
  humano juega la partida y el observador no participa, por lo que no hay ventaja competitiva que
  proteger. Esto reemplaza explícitamente la vista "neutral" (sin cartas) del espectado de partidas
  humanas.
- **Ocupación acotada en el tiempo**: una partida bot-vs-bot progresa automáticamente y concluye en
  un tiempo acotado, por lo que la ocupación del creador es naturalmente limitada; no se contempla
  una cancelación manual anticipada en esta feature.
- **Reutilización de mecanismos existentes**: se reutilizan el catálogo de bots, el motor de
  decisiones de los bots, la noción de disponibilidad/ocupación de jugadores y la infraestructura de
  espectado ya existentes.
- **Formato por defecto**: si el formato de serie no se especifica explícitamente, el default
  sensato es "mejor de 3".
- **Definición de "ocupado"**: un usuario está ocupado si participa de una partida, está espectando,
  o tiene una partida bot-vs-bot que él creó en curso; cualquiera de estos estados lo vuelve no
  disponible para crear una nueva partida bot-vs-bot.
