# Feature Specification: Estado de presencia / ocupación del usuario

**Feature Branch**: `008-user-presence`

**Created**: 2026-06-04

**Status**: Draft

**Input**: User description: "Endpoint para devolver si un usuario está en un match/liga/copa u
ocupado en general, con el id, o lo que necesite el FE para acceder a eso y poder reconectarse."

## Resumen

El frontend necesita una forma de saber, tras un refresco de página o reconexión, **dónde está
ocupado** el usuario autenticado para poder llevarlo de vuelta al recurso correcto (la partida en
curso, el lobby de la liga, la copa, o la pantalla de revancha). Hoy esa información existe
dispersa en cada dominio pero no hay un único punto de consulta que la agregue.

Esta feature expone un endpoint de **presencia** que reúne, para el usuario autenticado, el estado
de ocupación en los dominios que **sobreviven a una desconexión**: partida (match), liga, copa y
revancha (rematch), incluyendo los identificadores necesarios para reconectarse.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconexión a una partida en curso (Priority: P1)

Un jugador está jugando una partida, se le corta la conexión o refresca la página. Al volver a
cargar la aplicación, el frontend consulta el estado de presencia y descubre que el usuario tiene
una partida activa, con su identificador, y lo reconecta directamente a esa partida.

**Why this priority**: Es el caso de reconexión más frecuente y de mayor impacto: perder el acceso
a una partida en curso degrada gravemente la experiencia y puede derivar en abandono/derrota.

**Independent Test**: Con un usuario que tiene una partida en estado activo (esperando jugadores o
en juego), consultar el endpoint de presencia y verificar que devuelve el identificador de la
partida y su estado, sin requerir ningún otro dominio.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con una partida en curso, **When** consulta su presencia,
   **Then** la respuesta indica que está ocupado e incluye el identificador y el estado de esa
   partida.
2. **Given** un usuario autenticado sin ninguna partida activa, **When** consulta su presencia,
   **Then** la respuesta indica que no hay partida activa para ese dominio.

---

### User Story 2 - Reconexión a una liga o copa (Priority: P2)

Un jugador se unió a una liga o copa (ya sea esperando que comience o ya en progreso) y se
desconecta. Al volver, el frontend consulta la presencia y lo devuelve al lobby o a la partida
actual del torneo correspondiente.

**Why this priority**: Las ligas y copas persisten ante desconexiones (el jugador sigue siendo
miembro), por lo que es importante reconducirlo al torneo. Es menos frecuente que una partida
suelta pero igual de necesario para una experiencia consistente.

**Independent Test**: Con un usuario inscripto en una liga (esperando o en progreso), consultar el
endpoint y verificar que devuelve el identificador de la liga, su estado y, si está en progreso, el
identificador de la partida actual del torneo. Repetir análogamente para copa.

**Acceptance Scenarios**:

1. **Given** un usuario inscripto en una liga aún no iniciada, **When** consulta su presencia,
   **Then** la respuesta incluye el identificador de la liga y su estado de espera.
2. **Given** un usuario en una liga en progreso, **When** consulta su presencia, **Then** la
   respuesta incluye el identificador de la liga, su estado en progreso y el identificador de la
   partida actual del torneo.
3. **Given** un usuario inscripto/participando en una copa, **When** consulta su presencia, **Then**
   la respuesta incluye el identificador de la copa, su estado y, si corresponde, la partida actual.

---

### User Story 3 - Retorno a una revancha pendiente (Priority: P3)

Tras finalizar una partida, el usuario tiene una sesión de revancha abierta pendiente de su
decisión. Si se desconecta antes de decidir, al volver el frontend lo lleva a la pantalla de
revancha.

**Why this priority**: Es un caso acotado y de ventana temporal corta, pero forma parte del estado
de ocupación que el usuario puede querer retomar.

**Independent Test**: Con un usuario que tiene una sesión de revancha abierta, consultar el endpoint
y verificar que devuelve el identificador de la sesión de revancha y el de la partida de origen.

**Acceptance Scenarios**:

1. **Given** un usuario con una sesión de revancha abierta, **When** consulta su presencia, **Then**
   la respuesta incluye el identificador de la sesión de revancha y el de la partida de origen.

---

### Edge Cases

- **Usuario totalmente libre**: si el usuario no está en ninguna partida, liga, copa ni revancha, la
  respuesta debe indicar explícitamente que **no está ocupado** (indicador general en falso y todos
  los dominios vacíos), sin error.
- **Ocupación simultánea coherente**: cuando el usuario está en una partida que pertenece a una liga
  o copa, la respuesta refleja **ambos** dominios (el torneo y la partida actual), de modo que el
  frontend entienda que esa partida es la del torneo.
- **Sin autenticación**: una solicitud sin un usuario autenticado válido debe ser rechazada como no
  autorizada, igual que el resto de recursos protegidos del sistema.
- **Búsqueda de partida rápida (quick match)**: se considera **fuera de alcance** porque no
  sobrevive a una desconexión (al cortarse la conexión el usuario sale automáticamente de la cola),
  por lo que nunca sería un estado reconectable tras un refresco.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer una consulta de presencia que opere **sobre el usuario
  autenticado** que realiza la solicitud, sin recibir el identificador de usuario como parámetro
  externo (para no exponer estado de terceros).
- **FR-002**: El sistema MUST rechazar como no autorizada cualquier solicitud de presencia que no
  provenga de un usuario autenticado válido.
- **FR-003**: La consulta MUST reportar, para el dominio **partida**, si el usuario tiene una
  partida no finalizada (esperando jugadores o en juego) y, de existir, su identificador y estado.
- **FR-004**: La consulta MUST reportar, para el dominio **liga**, si el usuario está inscripto en
  una liga en espera o en progreso y, de existir, su identificador, su estado y —cuando esté en
  progreso— el identificador de la partida actual del torneo.
- **FR-005**: La consulta MUST reportar, para el dominio **copa**, si el usuario está inscripto/
  participando en una copa en espera o en progreso y, de existir, su identificador, su estado y
  —cuando corresponda— el identificador de la partida actual.
- **FR-006**: La consulta MUST reportar, para el dominio **revancha**, si el usuario tiene una
  sesión
  de revancha abierta y, de existir, su identificador y el de la partida de origen.
- **FR-007**: La consulta MUST exponer un **indicador general de ocupación** que sea verdadero si y
  solo si al menos uno de los dominios reportados tiene contenido.
- **FR-008**: Cuando el usuario no esté ocupado en un dominio, la respuesta MUST representar ese
  dominio como vacío (ausencia de datos) de forma inequívoca, en lugar de omitir silenciosamente la
  información de manera ambigua.
- **FR-009**: La consulta MUST ser de **solo lectura**: no modifica el estado de ninguna partida,
  liga, copa ni revancha (no une, no abandona, no reinicia temporizadores de inactividad como efecto
  de consultarla).
- **FR-010**: La consulta NO MUST incluir el estado de búsqueda de partida rápida (quick match), por
  estar fuera de alcance según las reglas de reconexión.

### Key Entities *(include if feature involves data)*

- **Presencia del usuario**: representación agregada del estado de ocupación del usuario
  autenticado. Atributos: indicador general de ocupación, y referencias opcionales a la partida,
  liga, copa y revancha activas.
- **Referencia de partida activa**: identificador de la partida y su estado actual.
- **Referencia de liga activa**: identificador de la liga, su estado y, opcionalmente, el
  identificador de la partida actual del torneo.
- **Referencia de copa activa**: identificador de la copa, su estado y, opcionalmente, el
  identificador de la partida actual.
- **Referencia de revancha activa**: identificador de la sesión de revancha y el identificador de la
  partida de origen.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con una sola consulta, el frontend puede determinar si el usuario está ocupado y en
  qué dominio, sin necesidad de consultar cada dominio por separado.
- **SC-002**: Para un usuario con una partida en curso, la respuesta provee el identificador
  necesario para reconectarse a esa partida en el 100% de los casos.
- **SC-003**: Para un usuario inscripto en una liga o copa (en espera o en progreso), la respuesta
  provee el identificador del torneo en el 100% de los casos.
- **SC-004**: Para un usuario sin ocupación alguna, la respuesta indica de forma inequívoca que no
  está ocupado, sin falsos positivos.
- **SC-005**: La consulta no produce cambios observables en el estado de ningún recurso
  (verificable: consultar repetidamente no altera partidas, ligas, copas, revanchas ni sus
  temporizadores de inactividad).

## Assumptions

- La identidad del usuario se obtiene del mecanismo de autenticación ya existente en el sistema; se
  reutiliza sin cambios.
- "No finalizada" para una partida abarca los estados no terminales (esperando jugadores o en
  juego), excluyendo finalizadas y canceladas; este criterio es consistente con la noción de
  ocupación reconectable usada en el resto del sistema.
- Partida, liga, copa y revancha **persisten** ante una desconexión: el usuario solo deja de estar
  asociado mediante una salida explícita o por los procesos de expiración por inactividad ya
  existentes; por lo tanto son estados legítimamente reconectables.
- La búsqueda de partida rápida (quick match) **no persiste** ante una desconexión, por lo que queda
  fuera de alcance de esta feature.
- El consumidor de este endpoint es el frontend del juego, que ya conoce cómo navegar a una partida,
  liga, copa o revancha dado su identificador.
