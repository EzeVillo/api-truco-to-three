# Feature Specification: Presencia en tiempo real (push de ocupación a sesiones activas)

**Feature Branch**: `009-presence-push`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: "Vamos con la A. La idea es que si tenés la sesión iniciada en más de
un lugar, en la que no usaste para entrar en un match, por ejemplo, pueda enterarse y te derive ahí,
ya que no podrías hacer otra cosa."

## Resumen

La feature [008-user-presence](../008-user-presence/spec.md) expone una consulta de presencia de
tipo **pull** (`GET /api/me/presence`): el frontend pregunta "¿dónde estoy ocupado?" en un momento
puntual, típicamente al recargar o reconectar. Esa consulta resuelve el **arranque en frío**, pero
no mantiene sincronizadas a las sesiones que ya están abiertas mientras el estado cambia.

Un mismo usuario puede tener la sesión iniciada en **más de un lugar a la vez** (varias pestañas o
dispositivos). Cuando desde **una** de esas sesiones entra a una partida —o cuando su liga/copa
arranca y lo mete en la partida del torneo, o se abre una revancha—, las **otras** sesiones quedan
desactualizadas: siguen mostrando el lobby o el home como si el usuario estuviera libre, cuando en
realidad ya está ocupado y **no puede hacer otra cosa** hasta resolver esa ocupación.

Esta feature agrega el modelo **push**: cuando la ocupación del usuario **cambia** en un dominio
reconectable (partida, liga, copa, revancha), el sistema **notifica a todas las sesiones activas de
ese usuario** —incluida la que originó el cambio— con la información necesaria para que cada sesión
se sincronice y derive al recurso correcto. Es la contraparte en tiempo real de la 008: la 008 da la
foto al conectar; esta feature transmite los cambios posteriores.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - La sesión ociosa se entera de la partida y deriva ahí (Priority: P1)

Un usuario tiene la aplicación abierta en dos lugares (por ejemplo, el teléfono y la computadora).
Desde uno de ellos entra a una partida. El otro lugar, que estaba en el lobby/home, recibe en el
acto un aviso de que el usuario quedó ocupado en una partida, con su identificador, y lo deriva a
esa misma partida —porque mientras esté en una partida no puede hacer otra cosa.

**Why this priority**: Es el núcleo del pedido y el caso de mayor impacto. Sin esto, una sesión
paralela puede iniciar acciones inválidas (buscar otra partida, unirse a otra cosa) mientras el
usuario ya está comprometido en una partida, generando estados inconsistentes y mala experiencia.

**Independent Test**: Con un usuario con dos sesiones activas, provocar el ingreso a una partida
desde la sesión A y verificar que la sesión B recibe, sin intervención manual, una notificación que
incluye el identificador y el estado de esa partida.

**Acceptance Scenarios**:

1. **Given** un usuario con dos sesiones activas, ambas mostrando el home, **When** desde la sesión
   A
   entra a una partida, **Then** la sesión B recibe una notificación de presencia que indica que el
   usuario quedó ocupado en una partida e incluye su identificador y estado.
2. **Given** un usuario con dos sesiones activas, **When** entra a una partida desde la sesión A,
   **Then** la sesión A también recibe la notificación (la entrega es a todas las sesiones, sin
   excluir a la originante), de modo que el resultado es idéntico independientemente de cuál sesión
   actuó.
3. **Given** un usuario con una sola sesión activa, **When** entra a una partida, **Then** esa misma
   sesión recibe la notificación de presencia coherente con su nuevo estado de ocupación.

---

### User Story 2 - La liga o copa arranca y todas las sesiones reciben la partida del torneo (Priority: P2)

Un usuario inscripto en una liga o copa está esperando que comience. Cuando el torneo arranca (o
avanza a la siguiente partida), todas sus sesiones activas reciben el aviso con el identificador de
la partida actual del torneo, para llevarlo a jugar.

**Why this priority**: El arranque de un torneo es un evento que el usuario no dispara él mismo
(ocurre cuando se completan los participantes o lo inicia el host), por lo que sus sesiones no
tienen forma de enterarse sin un push. Es importante para no perderse el inicio de la partida del
torneo, aunque es menos frecuente que entrar a una partida suelta.

**Independent Test**: Con un usuario inscripto en una liga en espera y con una sesión activa,
provocar el inicio del torneo y verificar que la sesión recibe una notificación con el identificador
de la liga, su nuevo estado y el identificador de la partida actual del torneo. Repetir para copa.

**Acceptance Scenarios**:

1. **Given** un usuario en una liga en espera con sesiones activas, **When** la liga pasa a en
   progreso, **Then** sus sesiones reciben una notificación con el identificador de la liga, su
   estado en progreso y el identificador de la partida actual del torneo.
2. **Given** un usuario en una copa en progreso, **When** la copa avanza a una nueva partida del
   torneo, **Then** sus sesiones reciben una notificación con el identificador de la copa y el de la
   nueva partida actual.

---

### User Story 3 - Se abre una revancha y las sesiones lo saben (Priority: P3)

Tras finalizar una partida que da lugar a una revancha, el usuario tiene una sesión de revancha
abierta pendiente de su decisión. Todas sus sesiones activas reciben el aviso para llevarlo a la
pantalla de revancha.

**Why this priority**: Caso acotado y de ventana corta, pero forma parte del estado de ocupación
reconectable; mantener las sesiones sincronizadas evita que una sesión muestre el home mientras hay
una revancha pendiente que requiere decisión.

**Independent Test**: Con un usuario con una sesión activa, provocar la apertura de una revancha y
verificar que la sesión recibe una notificación con el identificador de la sesión de revancha y el
de la partida de origen.

**Acceptance Scenarios**:

1. **Given** un usuario con sesiones activas y una partida recién finalizada que habilita revancha,
   **When** se abre la sesión de revancha, **Then** sus sesiones reciben una notificación con el
   identificador de la revancha y el de la partida de origen.

---

### User Story 4 - El usuario se libera y las sesiones vuelven al home (Priority: P3)

Cuando la ocupación del usuario termina (la partida finaliza, la liga/copa concluye o la revancha se
resuelve/expira) y el usuario ya no está ocupado en ese dominio, sus sesiones activas reciben el
aviso de que el dominio quedó libre, para volver al lobby/home en lugar de quedar atascadas en una
pantalla de un recurso que ya no existe.

**Why this priority**: Completa la simetría del modelo: tan importante como derivar a un recurso es
saber cuándo dejar de mostrarlo. Es P3 porque el desbloqueo también puede recuperarse con la
consulta pull de la 008, pero el push mejora la fluidez.

**Independent Test**: Con un usuario ocupado en una partida y con sesiones activas, provocar la
finalización de la partida y verificar que las sesiones reciben una notificación que indica que ese
dominio quedó libre.

**Acceptance Scenarios**:

1. **Given** un usuario ocupado en una partida con sesiones activas, **When** la partida finaliza,
   **Then** sus sesiones reciben una notificación indicando que el dominio partida quedó libre.
2. **Given** un usuario sin más ocupación en ningún dominio, **When** se libera la última ocupación,
   **Then** la notificación permite a las sesiones determinar que el usuario volvió a estar libre.

---

### Edge Cases

- **Sesión recién conectada (arranque en frío)**: el push solo cubre los cambios ocurridos
  **después** de que la sesión está activa y suscripta. Una sesión que se conecta debe obtener su
  estado inicial mediante la consulta pull de la 008; esta feature **complementa**, no reemplaza,
  esa
  consulta. La 008 sigue siendo la fuente de verdad para reconciliar.
- **Entrega no garantizada / evento perdido**: si una sesión no recibe una notificación (corte
  momentáneo, reconexión del canal), el estado correcto siempre puede recuperarse con la consulta
  pull de la 008. El diseño NO debe asumir entrega exactamente-una-vez ni que el frontend dependa
  exclusivamente del stream para conocer su estado.
- **Sin sesiones activas**: si en el momento del cambio el usuario no tiene ninguna sesión activa,
  no
  hay a quién notificar; el cambio simplemente queda disponible para la próxima consulta pull. No es
  un error.
- **Ocupación que pertenece a un torneo**: cuando el usuario entra a la partida de una liga/copa,
  las
  notificaciones deben reflejar de forma coherente **ambos** dominios (el torneo y la partida
  actual), igual que la coherencia exigida por la 008 (`match.id == league.currentMatchId`).
- **Búsqueda de partida rápida (quick match)**: la **cola** de quick match sigue **fuera de alcance
  **
  como estado de presencia (no sobrevive a una desconexión). Sin embargo, cuando la cola resuelve y
  crea una partida, esa **partida** sí se notifica como cualquier otra ocupación de partida.
- **Privacidad / aislamiento entre usuarios**: las notificaciones de presencia de un usuario MUST
  llegar **únicamente** a las sesiones de ese mismo usuario, nunca a las de terceros.
- **Sin autenticación**: una sesión que no esté autenticada con un usuario válido no puede
  suscribirse
  ni recibir notificaciones de presencia.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST notificar en tiempo real un **cambio de presencia** a **todas las
  sesiones activas autenticadas** de un usuario cada vez que su estado de ocupación cambie en un
  dominio reconectable (partida, liga, copa o revancha).
- **FR-002**: La notificación MUST entregarse también a la sesión que originó el cambio, de modo que
  el comportamiento sea independiente de cuál sesión disparó la acción (entrega idempotente: la
  sesión originante ya está en el recurso y la notificación es, para ella, un no-op).
- **FR-003**: Cada notificación MUST incluir la información necesaria para que una sesión navegue al
  recurso: el dominio afectado y los identificadores/estado correspondientes, con el mismo
  significado que la consulta pull de la 008 (partida: id + estado; liga/copa: id + estado +
  identificador de la partida actual del torneo cuando esté en progreso; revancha: id +
  identificador
  de la partida de origen).
- **FR-004**: El sistema MUST notificar cuando el usuario **pasa a estar ocupado** en una partida no
  finalizada (esperando jugadores, lista o en juego), incluyendo cuando esa partida proviene del
  arranque/avance de una liga o copa, o de la resolución de una búsqueda de partida rápida.
- **FR-005**: El sistema MUST notificar cuando una **liga o copa** del usuario cambia de estado de
  forma relevante para la ocupación (pasa a en progreso, o avanza a una nueva partida del torneo),
  incluyendo el identificador de la partida actual del torneo.
- **FR-006**: El sistema MUST notificar cuando se **abre una sesión de revancha** para el usuario,
  incluyendo su identificador y el de la partida de origen.
- **FR-007**: El sistema MUST notificar cuando el usuario **deja de estar ocupado** en un dominio (
  la
  partida finaliza, la liga/copa concluye, o la revancha se resuelve/expira), de forma que las
  sesiones puedan abandonar la pantalla de ese recurso.
- **FR-008**: Las notificaciones de presencia de un usuario MUST entregarse **exclusivamente** a las
  sesiones de ese mismo usuario y nunca a las de terceros.
- **FR-009**: El sistema MUST rechazar la suscripción o entrega de notificaciones de presencia a
  cualquier sesión que no esté autenticada con un usuario válido.
- **FR-010**: La emisión de notificaciones de presencia MUST ser un efecto **de solo lectura**
  respecto del dominio: reflejar cambios que ya ocurrieron, sin modificar el estado de ninguna
  partida, liga, copa ni revancha, y sin reiniciar temporizadores de inactividad.
- **FR-011**: Esta feature MUST **complementar** la consulta pull de la 008, no reemplazarla: una
  sesión recién conectada obtiene su estado inicial con la consulta pull, y el stream cubre los
  cambios posteriores. El diseño MUST permitir que el frontend reconcilie su estado mediante la
  consulta pull si pierde un evento.
- **FR-012**: La **cola** de búsqueda de partida rápida (quick match) MUST quedar fuera de alcance
  como estado de presencia notificable; solo la partida resultante (ya creada) se notifica.
- **FR-013**: La notificación de un cambio de ocupación que involucra una partida de torneo MUST ser
  **coherente** entre dominios: el identificador de la partida coincide con el identificador de la
  partida actual del torneo correspondiente.

### Key Entities *(include if feature involves data)*

- **Cambio de presencia (notificación)**: representación de una transición en la ocupación del
  usuario. Atributos: tipo de cambio (pasó a ocupado / se actualizó / quedó libre), dominio afectado
  (partida, liga, copa, revancha) y las referencias necesarias para navegar (identificadores y
  estado), con el mismo significado que las referencias de la 008.
- **Sesión activa del usuario**: cada conexión autenticada del usuario capaz de recibir
  notificaciones en tiempo real. Un usuario puede tener varias simultáneamente; todas reciben las
  mismas notificaciones de presencia.
- **Referencias de ocupación** (reutilizadas de la 008): referencia de partida activa (id + estado),
  de liga activa (id + estado + partida actual), de copa activa (id + estado + partida actual) y de
  revancha activa (id + partida de origen).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cuando un usuario con varias sesiones activas entra a una partida desde una de ellas,
  el resto de sus sesiones refleja la ocupación sin intervención manual (sin recargar) en el 100% de
  los casos en que estaban conectadas al momento del cambio.
- **SC-002**: Una sesión ociosa que recibe la notificación cuenta con el identificador necesario
  para
  derivar a la partida activa en el 100% de los casos.
- **SC-003**: Las notificaciones de presencia se entregan únicamente a las sesiones del usuario
  dueño
  del estado; cero filtraciones hacia sesiones de otros usuarios.
- **SC-004**: El arranque o avance de una liga/copa, y la apertura de una revancha, se reflejan en
  todas las sesiones activas del usuario con el identificador del recurso correspondiente.
- **SC-005**: La emisión y el consumo de notificaciones de presencia no producen ningún cambio
  observable en el estado de partidas, ligas, copas, revanchas ni en sus temporizadores de
  inactividad (verificable de forma análoga a la 008).
- **SC-006**: Una sesión que se conecta después de un cambio (o que perdió el evento) puede
  recuperar
  el estado de ocupación correcto mediante la consulta pull de la 008, sin depender de haber
  recibido
  la notificación en vivo.

## Assumptions

- La identidad del usuario y el mecanismo de autenticación de las sesiones en tiempo real son los ya
  existentes en el sistema; se reutilizan sin cambios.
- Existe ya un canal de comunicación en tiempo real por usuario (el mismo que usan las
  notificaciones del sistema) sobre el cual entregar las notificaciones de presencia; no se crea un
  mecanismo de transporte nuevo.
- Los dominios partida, liga, copa y revancha ya emiten señales internas cuando su estado cambia
  (inicio, avance, finalización, apertura de revancha); esta feature **escucha** esas señales y las
  proyecta como notificaciones de presencia, sin introducir nueva lógica de negocio en esos
  dominios.
- La entrega de notificaciones es **best-effort**: ante pérdida o reconexión, la consulta pull de la
  008 es la red de seguridad para reconstruir el estado.
- La decisión de **cómo** una sesión reacciona a una notificación (derivar automáticamente, bloquear
  la interfaz, ofrecer un aviso) es responsabilidad del frontend; esta feature solo provee la señal
  y
  los datos necesarios.
- "No finalizada" para una partida abarca los mismos estados no terminales que la 008 (esperando
  jugadores, lista, en juego), excluyendo finalizadas y canceladas.
- El consumidor de estas notificaciones es el frontend del juego, que ya sabe navegar a una partida,
  liga, copa o revancha dado su identificador (igual que en la 008).
