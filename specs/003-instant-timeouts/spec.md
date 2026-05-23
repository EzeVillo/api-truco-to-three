# Feature Specification: Timeouts instantáneos por entidad

**Feature Branch**: `003-instant-timeouts`

**Created**: 2026-05-22

**Status**: Draft

**Input**: User description: "necesito que los timeout de todo el proyecto no sean por un job, sino
que match, en el caso de TimeoutIdleMatchesUseCase, tenga su propio time out, ya que necesito que
sea exactamente en el momento el timeout, y no que si queda fuera del job por x tiempo, tener que
esperar al siguiente job, aplicar esta logica a todos los timeouts ademas de match, como por
ejemplo el de cup, league, etc"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Timeout exacto de partida inactiva (Priority: P1)

Como jugador participando de una partida de truco, cuando un oponente deja de jugar y supera el
límite de inactividad permitido, la partida debe finalizar por timeout **en el instante exacto en
que se cumple el plazo**, sin tener que esperar a que un proceso periódico la detecte.

**Why this priority**: Es la fuente original del problema reportado. Hoy, una partida con timeout
configurado en X segundos puede tardar hasta X + intervalo del job en cerrarse, lo que produce
esperas perceptibles, mensajes confusos para los jugadores y métricas de tiempo de resolución
inconsistentes. La partida es la entidad de mayor frecuencia de uso y, por lo tanto, donde el costo
percibido del retraso es más alto.

**Independent Test**: Iniciar una partida, dejarla inactiva el tiempo justo configurado como
timeout y verificar que la finalización por timeout ocurre dentro de un margen de ±1 segundo
respecto al momento esperado, independientemente del intervalo histórico del job (30 s).

**Acceptance Scenarios**:

1. **Given** una partida activa con timeout de inactividad de N segundos, **When** la partida pasa
   N segundos sin actividad de los jugadores, **Then** el sistema finaliza la partida por timeout
   dentro de los 1000 ms posteriores al cumplimiento del plazo.
2. **Given** una partida cuya última acción está a punto de cumplir el timeout, **When** un jugador
   realiza una acción válida antes de cumplirse el plazo, **Then** el plazo se reinicia desde la
   nueva acción y la partida no se cierra por timeout.
3. **Given** una partida programada para finalizar por timeout, **When** la partida termina
   normalmente (por puntaje, rendición o desconexión definitiva) antes de cumplirse el plazo,
   **Then** el sistema cancela el timeout pendiente y no produce una segunda finalización ni
   eventos duplicados.

---

### User Story 2 - Timeout exacto en torneos: copa y liga (Priority: P2)

Como organizador o participante de una copa o liga, cuando un cruce/jornada queda inactivo y supera
el plazo permitido, la transición automática (avance, eliminación o cierre) debe ocurrir en el
momento exacto del vencimiento, no cuando el siguiente ciclo del job lo detecte.

**Why this priority**: Copa y liga heredan exactamente el mismo problema que partida (jobs cada
60 s) y comparten infraestructura. Su frecuencia es menor, pero el costo percibido en torneos en
vivo y la coherencia con la corrección hecha en partida hacen razonable resolverlos en el mismo
trabajo.

**Independent Test**: Forzar una copa y una liga al borde del plazo de inactividad y verificar que
la transición automática se ejecuta dentro de ±1 segundo del vencimiento, no del intervalo del job.

**Acceptance Scenarios**:

1. **Given** una copa con un cruce inactivo cuyo plazo de timeout es N segundos, **When** se
   cumplen N segundos sin actividad válida, **Then** el sistema aplica la resolución por timeout
   correspondiente al cruce dentro de los 1000 ms posteriores al cumplimiento del plazo.
2. **Given** una liga con una jornada/partida inactiva sujeta a timeout, **When** se cumple el
   plazo, **Then** el sistema aplica la transición automática (cierre/avance) dentro de los
   1000 ms posteriores al vencimiento.
3. **Given** una copa o liga con timeout programado, **When** ocurre una acción válida que reinicia
   el plazo (jugada, confirmación de avance, etc.), **Then** el timeout previo queda invalidado y
   se programa uno nuevo desde la actividad reciente.

---

### User Story 3 - Timeout exacto en invitaciones y rematch (Priority: P3)

Como usuario que recibió una invitación social pendiente o una propuesta de rematch (revancha),
cuando se cumple el plazo de expiración, la invitación/sesión debe marcarse como expirada
exactamente en ese momento, para que el remitente y el destinatario vean estados consistentes sin
demora.

**Why this priority**: Estas expiraciones hoy también dependen de jobs (rematch cada 10 s,
invitaciones sociales cada 60 s). La unificación bajo el mismo mecanismo evita inconsistencia
operativa y futuras divergencias, pero su impacto perceptual es menor que el de partida o torneos.

**Independent Test**: Crear una invitación social pendiente y una sesión de rematch ambas con
expiración cercana; al cumplirse el plazo, verificar que el cambio de estado ocurre dentro de
±1 segundo del vencimiento esperado.

**Acceptance Scenarios**:

1. **Given** una invitación social pendiente con expiración programada a un instante T, **When**
   llega el instante T, **Then** el sistema marca la invitación como expirada y notifica a los
   participantes dentro de los 1000 ms posteriores a T.
2. **Given** una sesión de rematch con vencimiento en un instante T, **When** todos los
   participantes aceptan antes de T, **Then** el sistema cancela el timeout pendiente y la sesión
   procede sin disparar la expiración automática.
3. **Given** una sesión de rematch con vencimiento en un instante T, **When** llega T sin aceptación
   completa, **Then** el sistema expira la sesión exactamente en T (±1 s) y notifica el resultado.

---

### Edge Cases

- **Reinicio del servicio**: si el servicio se reinicia mientras hay timeouts pendientes, al
  arrancar debe reprogramar los timeouts que aún no vencieron y disparar de inmediato (o como
  máximo dentro de unos pocos segundos) los que ya hubiesen vencido durante la caída.
- **Plazo ya vencido al programarse**: si por una demora interna el momento de vencimiento ya pasó
  cuando se intenta programar el timeout, el sistema debe ejecutar la acción de inmediato (no
  esperar otro ciclo).
- **Acción concurrente al instante del vencimiento**: si una acción válida (jugada, confirmación,
  aceptación) ocurre prácticamente en simultáneo con la ejecución del timeout, el sistema debe
  garantizar que solo una de las dos transiciones se aplica (la primera en quedar consolidada) y
  evitar finalizaciones duplicadas o estados incoherentes.
- **Cancelación tardía**: si una entidad termina normalmente y el timeout ya estaba en curso de
  ejecutarse, el sistema debe descartar el efecto del timeout sin producir eventos duplicados.
- **Migración inicial**: durante el despliegue de la nueva lógica, deben procesarse correctamente
  las entidades que ya estaban creadas con el modelo anterior, sin perder ni duplicar timeouts.
- **Carga elevada**: ante muchas entidades con timeouts próximos entre sí, el sistema debe seguir
  cumpliendo el objetivo de exactitud sin acumular un atraso creciente.
- **Modo de despliegue con múltiples instancias**: si en algún momento corre más de una instancia
  del servicio, debe garantizarse que cada timeout se ejecute exactamente una vez en el sistema, sin
  duplicaciones entre instancias.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE disparar la finalización por timeout de una partida inactiva dentro
  de los 1000 ms posteriores al instante exacto en que se cumple su plazo de inactividad, sin
  depender de un ciclo de chequeo periódico.
- **FR-002**: El sistema DEBE disparar la resolución automática por timeout de una copa inactiva
  dentro de los 1000 ms posteriores al cumplimiento de su plazo.
- **FR-003**: El sistema DEBE disparar la transición automática por timeout de una liga inactiva
  dentro de los 1000 ms posteriores al cumplimiento de su plazo.
- **FR-004**: El sistema DEBE expirar invitaciones sociales pendientes dentro de los 1000 ms
  posteriores al instante de expiración configurado.
- **FR-005**: El sistema DEBE expirar sesiones de rematch dentro de los 1000 ms posteriores al
  instante de expiración configurado.
- **FR-006**: Cada vez que una entidad sujeta a timeout reciba una acción válida que reinicie su
  plazo, el sistema DEBE invalidar el timeout previo y programar uno nuevo basado en la actividad
  reciente.
- **FR-007**: Cuando una entidad finalice de forma natural (puntaje, rendición, aceptación,
  cancelación, etc.) antes del vencimiento, el sistema DEBE cancelar el timeout pendiente para esa
  entidad, garantizando que no se ejecute posteriormente.
- **FR-008**: Cuando el servicio se reinicie, el sistema DEBE reconciliar los timeouts pendientes
  de todas las entidades en estado activo: reprogramar los que aún no vencieron y disparar
  inmediatamente los que ya hubieran vencido durante la caída.
- **FR-009**: Ante una concurrencia entre la ejecución de un timeout y otra acción que también
  produciría una transición sobre la misma entidad, el sistema DEBE garantizar que solo una de las
  dos transiciones quede aplicada y que no se emitan eventos duplicados.
- **FR-010**: El sistema DEBE mantener observabilidad sobre los timeouts: registrar al menos cuándo
  se programa, cuándo se cancela y cuándo se ejecuta cada timeout, y exponer una señal de salud
  equivalente o superior a la actual basada en jobs, de modo que sea posible detectar fallas en el
  mecanismo de timeouts.
- **FR-011**: Los plazos de timeout configurables hoy (inactividad de partida, copa, liga;
  expiración de invitación social; expiración de rematch) DEBEN seguir siendo configurables sin
  modificación de código.
- **FR-012**: El sistema DEBE garantizar que cada timeout se ejecuta exactamente una vez para una
  entidad dada, incluso si el servicio corre con más de una instancia simultáneamente.
- **FR-013**: El sistema DEBE eliminar el mecanismo basado en jobs periódicos para los timeouts
  cubiertos por esta feature, de modo que el comportamiento observable no dependa del intervalo de
  ningún job de barrido. (Una verificación de respaldo de muy baja frecuencia para reconciliación
  no se considera el mecanismo principal y queda permitida si aporta robustez.)

### Key Entities *(include if feature involves data)*

- **Timeout programado**: representa la intención de aplicar una transición automática sobre una
  entidad de negocio en un instante futuro. Atributos relevantes: entidad asociada (tipo y
  referencia), instante de vencimiento, estado (pendiente, cancelado, ejecutado), motivo
  (inactividad de partida, expiración de invitación, etc.).
- **Entidad sujeta a timeout**: partida (match), copa (cup), liga (league), invitación social,
  sesión de rematch. Cada una posee su propio plazo y su propio instante de vencimiento, derivado
  de su última actividad relevante.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 99 % de las finalizaciones por timeout de partida ocurren dentro de los 1000 ms
  posteriores al instante teórico de vencimiento, medido sobre una semana de operación.
- **SC-002**: El retraso máximo observado entre el vencimiento teórico y la ejecución efectiva del
  timeout para cualquier entidad cubierta (partida, copa, liga, invitación social, rematch) es
  inferior a 3 segundos, salvo durante eventos de reinicio del servicio.
- **SC-003**: Tras la entrega, el retraso medio percibido por los jugadores entre "tiempo agotado"
  y "la partida terminó" se reduce a menos de 1 segundo (frente a hasta 30 segundos hoy en
  partida, hasta 60 en copa y liga).
- **SC-004**: Cero finalizaciones o expiraciones duplicadas por timeout en una semana de operación
  bajo carga normal.
- **SC-005**: Tras un reinicio del servicio, el 100 % de los timeouts ya vencidos durante la caída
  quedan procesados dentro de los primeros 10 segundos posteriores al arranque, y los pendientes
  no vencidos siguen disparándose dentro del margen de 1000 ms definido por SC-001/SC-002.
- **SC-006**: La cobertura del comportamiento (programación, cancelación, ejecución, reinicio,
  concurrencia) queda validada por pruebas automatizadas que se ejecutan como parte del pipeline
  existente.

## Assumptions

- Los plazos de inactividad y expiración actualmente configurados (timeouts de partida, copa, liga,
  invitación social, rematch) se conservan en sus valores actuales; esta feature cambia **cuándo
  se ejecuta** el timeout, no **cuánto dura** el plazo.
- Las acciones de negocio que reinician o cancelan un timeout ya existen y son detectables; esta
  feature se conecta a esas acciones, no las redefine.
- El servicio se ejecuta predominantemente con una sola instancia, pero el diseño no debe asumirlo:
  debe ser correcto también con múltiples instancias (FR-012).
- La persistencia subyacente de las entidades sujetas a timeout permite recuperar, al arrancar el
  servicio, las que están en estado activo junto con su instante de vencimiento, para reconciliar
  los timeouts pendientes (FR-008).
- Las métricas y verificaciones de salud actuales basadas en "el job de timeout corrió hace menos
  de X" deben ser reemplazadas o adaptadas; no se asume que se mantengan tal cual.
- "Todos los timeouts del proyecto" se interpreta como los cinco enumerados en este documento
  (partida, copa, liga, invitación social, rematch). Si en el futuro aparecen otros, se aplicarán
  bajo el mismo patrón pero quedan fuera del alcance inmediato.

## Documentación a actualizar

- `README.md`: si menciona el comportamiento basado en jobs de timeout o sus intervalos, debe
  actualizarse para reflejar que los timeouts ahora son instantáneos por entidad.
- `docs/CONTRATOS_API.md`: revisar si describe garantías de tiempo de respuesta de eventos
  WebSocket asociados a timeouts (partida, copa, liga, invitaciones, rematch) y, en caso
  afirmativo, actualizar las afirmaciones que dejan de ser válidas (por ejemplo, "hasta N segundos
  de retraso por el job de barrido"). No se prevén cambios de shape de endpoints ni de eventos
  WebSocket.
