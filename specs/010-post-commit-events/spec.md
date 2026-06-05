# Feature Specification: Notificaciones post-commit (fin de race conditions A y B)

**Feature Branch**: `010-post-commit-events`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: "solucionemos entonces todos los de la categoria a y b, y recordalo
para que no nos vuelva a pasar"

## Contexto

Hoy las notificaciones en tiempo real (pushes WebSocket) se emiten **dentro de la transacción** que
produjo el cambio, antes de que la base de datos confirme (commit). Sólo un puñado de eventos ya se
emite después del commit. Esto provoca dos clases de fallas observadas:

- **Categoría A — Lectura prematura (404 / datos viejos)**: el jugador recibe el aviso con el
  identificador de un recurso recién creado/actualizado (p. ej. un match de partida rápida) y al ir
  a buscarlo recibe un error de "no encontrado", porque la transacción aún no había confirmado. Al
  refrescar manualmente, ya funciona.
- **Categoría B — Aviso fantasma o duplicado**: el jugador recibe un aviso autocontenido (p. ej. un
  mensaje de chat) que luego no quedó persistido porque la transacción se revirtió, o lo recibe dos
  veces porque la operación se reintentó. El aviso salió antes del commit y el reintento corre por
  fuera de la transacción.

El objetivo es que **toda notificación al usuario salga únicamente después de que el cambio quede
confirmado en la base de datos**, y dejar establecida la convención para que esto no se
reintroduzca.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Encontrar partida rápida sin error (Priority: P1)

Un jugador en la cola de partida rápida es emparejado con otro. Recibe el aviso de "partida
encontrada" con el identificador de la partida y, al abrirla inmediatamente, la partida ya existe y
carga correctamente, sin necesidad de refrescar.

**Why this priority**: Es el caso reportado y reproducido. Afecta el flujo de entrada principal al
juego; el usuario percibe el producto como roto (recibe un aviso que lleva a un error).

**Independent Test**: Emparejar a dos jugadores en partida rápida y verificar que, en el instante en
que llega el aviso al oponente, la consulta del recurso devuelve la partida (no un "no encontrado").

**Acceptance Scenarios**:

1. **Given** dos jugadores en la cola de partida rápida, **When** se produce el emparejamiento,
   **Then** el aviso de partida encontrada llega a ambos sólo después de que la partida está
   persistida, y la consulta inmediata del recurso la devuelve correctamente.
2. **Given** un jugador que recibe el aviso de partida encontrada, **When** consulta la partida sin
   refrescar, **Then** nunca recibe un error de "no encontrado".

---

### User Story 2 - Navegar a cualquier recurso recién creado sin error (Priority: P2)

Cuando un jugador recibe un aviso que apunta a un recurso recién creado o avanzado (revancha
confirmada, liga o copa creada/iniciada, invitación aceptada que crea una partida, avance a la
siguiente partida de una competición), al abrirlo el recurso ya existe y carga correctamente.

**Why this priority**: Es la misma falla de categoría A en el resto de los flujos. Menos frecuente
que la partida rápida pero igual de visible cuando ocurre.

**Independent Test**: Para cada flujo que crea/avanza un recurso y notifica con su identificador,
verificar que la consulta inmediata tras el aviso devuelve el recurso.

**Acceptance Scenarios**:

1. **Given** una revancha confirmada por ambos jugadores, **When** se crea la nueva partida, **Then
   **
   el aviso con el nuevo identificador llega sólo tras la confirmación y la partida es consultable
   de
   inmediato.
2. **Given** una liga o copa que se crea o inicia, **When** llega el aviso correspondiente, **Then**
   el recurso (y su primera partida, si aplica) es consultable de inmediato.
3. **Given** una invitación a un recurso que se acepta y genera una partida, **When** llega el
   aviso,
   **Then** la partida es consultable de inmediato.
4. **Given** una competición que avanza a la siguiente partida al terminar la anterior, **When**
   llega
   el aviso de la nueva partida, **Then** ésta es consultable de inmediato.

---

### User Story 3 - No recibir avisos fantasma ni duplicados (Priority: P3)

Cuando una operación que genera un aviso falla y se revierte, el jugador no recibe ningún aviso de
ese cambio. Cuando una operación se reintenta internamente, el jugador recibe el aviso una sola vez.

**Why this priority**: Es la categoría B. Síntoma más sutil (no es un error visible como el 404)
pero
genera inconsistencias entre lo que el jugador ve en vivo y lo que el servidor realmente guardó.

**Independent Test**: Forzar el rollback de una operación que notifica (p. ej. enviar un mensaje de
chat) y verificar que no se emitió ningún aviso; forzar un reintento y verificar que el aviso se
emite exactamente una vez (tras el commit exitoso).

**Acceptance Scenarios**:

1. **Given** una operación que enviaría un aviso, **When** la transacción se revierte antes de
   confirmar, **Then** no se emite ningún aviso de ese cambio.
2. **Given** una operación que se reintenta por conflicto de concurrencia, **When** finalmente
   confirma, **Then** el aviso se emite una única vez y refleja el estado confirmado.

---

### User Story 4 - Que no se reintroduzca (Priority: P3)

Cualquier notificación al usuario incorporada en el futuro queda emitida post-commit por defecto,
sin
que cada desarrollador tenga que recordarlo manualmente.

**Why this priority**: Es el pedido explícito de "recordalo para que no nos vuelva a pasar". Sin una
salvaguarda, el problema reaparece con cada nuevo evento de notificación.

**Independent Test**: Agregar un evento de notificación nuevo sin marcarlo explícitamente y
verificar
(vía prueba automatizada o regla de arquitectura) que igualmente se emite post-commit, o que el
sistema obliga a una decisión consciente.

**Acceptance Scenarios**:

1. **Given** un evento de notificación al usuario nuevo, **When** se agrega al sistema, **Then** por
   defecto se comporta como post-commit sin configuración adicional.
2. **Given** la base de código actual, **When** se ejecuta la suite de pruebas, **Then** existe una
   verificación que detecta eventos de notificación que se emitirían antes del commit.

### Edge Cases

- **Sin transacción activa**: cuando un aviso se origina fuera de una transacción de escritura (p.
  ej.
  cambios de cantidad de espectadores disparados desde eventos de conexión/desconexión), debe
  emitirse igual, sin quedar retenido ni perderse.
- **Múltiples avisos en una misma operación**: si una operación genera varios avisos, todos deben
  emitirse tras el commit y preservar su orden relativo.
- **Fallo al enviar el aviso tras el commit**: si el envío del aviso falla después de un commit
  exitoso, el dato ya está persistido; el fallo de envío no debe revertir el cambio ni dejar el
  sistema inconsistente, y debe quedar registrado/observable.
- **Eventos de coordinación entre dominios (categoría C)**: los eventos que disparan **escrituras**
  atómicas en otros agregados (p. ej. avance de competición, logros, creación de sesión de revancha)
  quedan **fuera del alcance** de este cambio y deben seguir ejecutándose dentro de la transacción.
  Sólo la **notificación derivada** de esos flujos pasa a post-commit.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST emitir toda notificación dirigida a un usuario (pushes en tiempo real)
  únicamente después de que la transacción que originó el cambio haya confirmado (commit).
- **FR-002**: El sistema MUST aplicar lo anterior a las notificaciones de partida que hoy se emiten
  dentro de la transacción, cubriendo partida rápida, inicio de partida, revancha confirmada e
  invitación aceptada.
- **FR-003**: El sistema MUST aplicar lo anterior a las notificaciones de liga, copa y sociales
  asociadas a creación/avance/aceptación de recursos.
- **FR-004**: El sistema MUST aplicar lo anterior a las notificaciones autocontenidas afectadas por
  rollback/reintento (categoría B), incluyendo al menos las notificaciones de chat.
- **FR-005**: El sistema MUST garantizar que, si una operación se revierte, no se emite ninguna
  notificación derivada de esa operación.
- **FR-006**: El sistema MUST garantizar que, si una operación se reintenta, sus notificaciones se
  emiten una sola vez, correspondiente al intento que confirmó.
- **FR-007**: El sistema MUST seguir emitiendo notificaciones que se originan fuera de una
  transacción de escritura, sin retenerlas indefinidamente ni descartarlas.
- **FR-008**: El sistema MUST mantener dentro de la transacción los eventos de coordinación que
  disparan escrituras atómicas en otros agregados (categoría C), separando la escritura (in-tx) de
  la
  notificación derivada (post-commit).
- **FR-009**: El sistema MUST establecer una salvaguarda para que las notificaciones de usuario
  nuevas se comporten post-commit por defecto, o para que su omisión sea detectada automáticamente
  (p. ej. mediante prueba o regla de arquitectura).
- **FR-010**: El comportamiento observable de las notificaciones (contenido, destinatarios, orden
  relativo) MUST permanecer igual; lo único que cambia es el **momento** de emisión (tras el
  commit).
- **FR-011**: La documentación del proyecto MUST reflejar la convención de emisión post-commit como
  regla para nuevos eventos de notificación.

### Key Entities *(include if feature involves data)*

- **Notificación al usuario**: aviso en tiempo real dirigido a uno o más jugadores, con contenido,
  destinatarios y momento de emisión. Atributo nuevo relevante: debe emitirse post-commit.
- **Evento de coordinación entre dominios (categoría C)**: evento que dispara escrituras atómicas en
  otro agregado; permanece in-tx y queda fuera del alcance directo, salvo por su notificación
  derivada.
- **Operación transaccional**: unidad de cambio que puede confirmar, revertir o reintentarse; define
  el límite tras el cual deben emitirse las notificaciones.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el flujo de partida rápida, el 100% de los avisos de "partida encontrada" permiten
  consultar la partida de inmediato sin error de "no encontrado".
- **SC-002**: Cero ocurrencias del error de "no encontrado" inmediatamente posterior a un aviso de
  recurso recién creado/avanzado, en todos los flujos del alcance (A).
- **SC-003**: Ante una operación revertida, el 100% de las veces no se emite ninguna notificación
  derivada (sin avisos fantasma).
- **SC-004**: Ante una operación reintentada que finalmente confirma, cada notificación se emite
  exactamente una vez (sin duplicados).
- **SC-005**: Un evento de notificación de usuario nuevo agregado sin configuración explícita queda
  cubierto por la salvaguarda (se comporta post-commit o es detectado por la suite de pruebas).
- **SC-006**: La suite de pruebas existente sigue pasando y se agregan pruebas que cubren A, B y la
  salvaguarda.

## Assumptions

- El mecanismo de emisión post-commit ya existente en el proyecto se reutiliza y extiende; no se
  introduce una infraestructura paralela.
- Los reintentos por conflicto de concurrencia ocurren por fuera del límite transaccional (cada
  intento es una transacción nueva), por lo que emitir post-commit elimina los duplicados.
- Las notificaciones de espectadores que se originan fuera de una transacción de escritura pueden
  quedar como están si se confirma que no corren dentro de una transacción reintentable; se
  evaluarán
  caso por caso durante el plan.
- Los eventos de coordinación de categoría C (avance de competición, logros, creación de sesión de
  revancha) quedan fuera del alcance de cambio de timing, salvo por sus notificaciones derivadas.
- El alcance es de corrección de comportamiento (timing de emisión); no cambia el contenido ni el
  contrato de los avisos.
