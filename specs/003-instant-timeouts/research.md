# Research: Timeouts instantáneos por entidad

## 1. Mecanismo de scheduling

### Decisión

Usar **Spring `ThreadPoolTaskScheduler`** (in-memory) detrás de un puerto de aplicación
`TimeoutScheduler`. La base de datos sigue siendo la fuente autoritativa: cada entidad sujeta a
timeout expone un `deadline` derivado de su última actividad. El scheduler in-memory mantiene
`Map<EntityKey, ScheduledFuture<?>>` para poder cancelar y reprogramar O(1).

### Rationale

- **Exactitud requerida (≤1 s)**: `ScheduledThreadPoolExecutor` dispara con precisión de
  milisegundos en JVM moderna; muy superior al requerimiento.
- **Sin nueva infraestructura**: evita introducir Quartz, RabbitMQ delayed queue o cron externo,
  consistente con el principio de Simplicidad/YAGNI.
- **Escala suficiente**: ~10 k timeouts simultáneos se manejan sin esfuerzo; el coste de memoria
  por entrada es despreciable.
- **Multi-instancia segura por idempotencia**: cada handler (`TimeoutIdle*CommandHandler`) ya
  reabre el agregado en una transacción y aplica la transición solo si todavía corresponde
  (`if (cup.getStatus() == statusBefore) return;`). Si dos instancias disparan el mismo timeout
  en paralelo, una pierde la carrera por optimistic lock / status check y no produce evento
  duplicado.

### Alternativas consideradas

- **Quartz con JDBC store**: ofrece persistencia y cluster support de fábrica, pero agrega una
  dependencia pesada y un schema propio. Innecesario porque el deadline ya vive en las tablas de
  negocio y la idempotencia ya está resuelta a nivel agregado.
- **Cola de mensajes con delivery diferido (RabbitMQ delayed exchange / SQS visibility timeout)**:
  agrega un broker, complica desarrollo local y multiplica modos de falla. Rechazada por
  YAGNI.
- **Polling más frecuente (cada 1 s)**: cumpliría SC-001 pero a coste de carga de BD permanente y
  no escala bien con muchas entidades inactivas mezcladas con activas. Rechazada.

---

## 2. Persistencia del deadline

### Decisión

Reutilizar la información de **última actividad** ya presente en cada entidad y calcular el
deadline como `lastActivityAt + idleTimeoutDuration`. Cuando no exista columna equivalente
(invitación social, rematch session), aprovechar las columnas existentes
`expiresAt` / `expirationAt` que ya guían los schedulers actuales.

No se agregan tablas nuevas. No se agrega una "cola de timeouts" persistente: la cola es el
*query* `SELECT id, deadline FROM <tabla> WHERE status = 'ACTIVE'` ejecutado al arrancar y en el
reconciliador de seguridad.

### Rationale

- La fuente de verdad ya existe en cada tabla; duplicarla en una cola separada introduce riesgo
  de inconsistencia.
- Permite que la reconciliación de arranque sea simplemente "leer entidades activas y agendar".
- Mantiene el dominio puro (no obliga a las entidades a conocer una cola).

### Alternativas consideradas

- **Tabla `scheduled_timeouts` separada**: ofrecería auditoría más fácil pero requiere
  sincronización transaccional con la mutación del agregado (escribir en dos tablas a la vez).
  Rechazada por complejidad sin valor adicional dado que el deadline ya es derivable.

---

## 3. Reprogramación y cancelación ante actividad / cierre

### Decisión

Aprovechar los **eventos de dominio existentes** (p. ej. `MatchActionPerformed`, `CupAdvanced`,
`LeagueRoundCompleted`, `MatchFinalized`, `RematchAccepted`, `InvitationAccepted`, etc.) y agregar
un `TimeoutSchedulingEventHandler` en `application/eventhandlers` que:

- Ante un evento que reinicie el timeout: recalcula el nuevo deadline y llama
  `timeoutScheduler.schedule(key, newDeadline)` (la implementación cancela el future previo si
  existía y agenda uno nuevo).
- Ante un evento de cierre natural: llama `timeoutScheduler.cancel(key)`.

### Rationale

- Reutiliza el bus de eventos de dominio existente; no obliga a tocar agregados.
- Mantiene el dominio agnóstico al scheduler.
- Centraliza la política de "qué eventos reprograman, cuáles cancelan" en un solo handler.

### Alternativas consideradas

- **Inyectar `TimeoutScheduler` en los command handlers de cada acción**: dispersa la lógica de
  scheduling y obliga a tocar muchos handlers. Rechazada.
- **Poll-based reschedule (releer deadline cada tick)**: vuelve al modelo viejo. Rechazada.

---

## 4. Reconciliación al arranque

### Decisión

`TimeoutReconciliationRunner` implementa `ApplicationListener<ApplicationReadyEvent>` y, una vez
que el contexto está listo:

1. Para cada bounded context (match, cup, league, social, rematch), consulta IDs y deadlines de
   entidades en estado activo (los repositorios ya tienen métodos `findIdle*Ids(cutoff)`; se
   agrega un `findActive*WithDeadline()` o equivalente).
2. Para cada entrada:
    - Si `deadline <= now`: invoca el handler de timeout inmediatamente (en un pool acotado para
      no saturar el arranque).
    - Si `deadline > now`: `scheduler.schedule(key, deadline)`.

### Rationale

- Cumple FR-008 y SC-005 sin estado adicional.
- Idempotente: si se vuelve a ejecutar (p. ej. desde el reconciliador de seguridad), `schedule`
  reemplaza el future previo sin daño.

### Alternativas consideradas

- **Esperar al primer evento de actividad por entidad**: no cumple para entidades que ya quedaron
  inactivas durante la caída. Rechazada.

---

## 5. Red de seguridad periódica

### Decisión

Un único `@Scheduled` cada **5 minutos** ejecuta la misma rutina de reconciliación. Cubre el caso
patológico en que el future in-memory se pierda (excepción no capturada en el scheduler thread,
GC pause extraordinario, etc.).

### Rationale

- Ventana máxima de retraso en escenarios catastróficos: 5 min, en lugar de "hasta el próximo
  reinicio". Es un *defense in depth* barato.
- 5 min es suficientemente raro como para no constituir el mecanismo principal (FR-013) ni
  invalidar el objetivo de exactitud de SC-001/SC-002 en operación normal.

### Alternativas consideradas

- **Sin red de seguridad**: deja una clase de bug latente. Rechazada.
- **Intervalo más corto (1 min)**: ruido innecesario en condiciones normales. Rechazada.

---

## 6. Concurrencia entre timeout y acciones normales

### Decisión

Confiar en la **idempotencia a nivel agregado** que ya existe:

- `Match.timeoutForfeit()` devuelve `false` si el match ya finalizó.
- `Cup.cancel()` y `League.cancel()` comparan `statusBefore` y no emiten eventos si no hubo
  cambio.
- Rematch e invitaciones tienen verificaciones equivalentes (`if (status != PENDING) return;`).

Toda ejecución de timeout corre en una transacción reabriendo el agregado vía repositorio; el
lock optimista existente (versión JPA) protege contra updates concurrentes desde otra acción.

### Rationale

- No introduce nuevo locking ni mecanismos distribuidos.
- Aprovecha la lógica ya probada de los command handlers actuales.

### Alternativas consideradas

- **Lock distribuido (Redis / advisory locks de Postgres)**: complejidad y latencia adicional sin
  beneficio frente a la idempotencia ya implementada. Rechazada.

---

## 7. Configuración del pool

### Decisión

`ThreadPoolTaskScheduler` con:

- `poolSize`: configurable, default **4** (suficiente para disparar timeouts; el trabajo pesado
  ocurre en transacciones que delegan en otros threads del request pool si fuese necesario,
  aunque en este caso corren en el scheduler thread mismo).
- `awaitTerminationSeconds`: 30, para drenar timeouts inminentes en shutdown.
- `setWaitForTasksToCompleteOnShutdown(true)`.

Propiedades: `truco.timeout.scheduler.pool-size`, `truco.timeout.safety-net-interval`.

### Rationale

- 4 hilos cubren picos esperados (varios disparos por segundo) sin gastar memoria.
- Drenado en shutdown reduce trabajo huérfano para la reconciliación de arranque.

---

## 8. Observabilidad

### Decisión

- Mantener `SchedulerHeartbeatRegistry` adaptado: registrar un *heartbeat* en cada disparo y en
  el reconciliador de seguridad (cada 5 min). El indicador de health pasa de "el job corrió hace
  ≤X" a "el scheduler está vivo y dispara cuando le toca".
- Logs `INFO` en programación, cancelación y ejecución de cada timeout (con `entityType` e
  `entityId`).
- Métricas Micrometer:
    - `truco.timeout.scheduled` (counter, tagged por `entityType`)
    - `truco.timeout.fired` (counter, tagged por `entityType`)
    - `truco.timeout.cancelled` (counter)
    - `truco.timeout.lag_ms` (timer / distribution summary): diferencia entre vencimiento teórico
      y ejecución efectiva. Permite verificar SC-001/SC-002 en producción.
    - `truco.timeout.pending` (gauge): tamaño actual del mapa in-memory.

### Rationale

- `lag_ms` es la métrica clave para demostrar la propuesta de valor sin ruido.
- Compatibilidad con stack de monitoring existente.

---

## 9. Testing

### Decisión

- **Unit**: `SpringTimeoutScheduler` con `Clock` y `TaskScheduler` falsos; verifica
  `schedule`/`cancel`/`reschedule` semánticamente.
- **Unit**: `TimeoutSchedulingEventHandler` con scheduler mockeado; verifica que cada evento de
  dominio dispara el método esperado.
- **Integration (Spring slice + H2)**: `TimeoutExactnessIT` crea una entidad real con deadline a
  ~2 s en el futuro y usa **Awaitility** para verificar que la transición de estado ocurre en
  `≤ deadline + 1 s` y `≥ deadline - 100 ms`.
- **Integration**: `TimeoutReconciliationIT` siembra una entidad con deadline ya vencido y otra
  futura antes de cargar el contexto; verifica que ambas se procesan correctamente.
- **Idempotencia / concurrencia**: test que dispara dos veces el handler con el mismo `id` y
  verifica que solo se emite un evento.

Títulos de tests en español por Principio IV.

### Rationale

- Awaitility permite testear exactitud real sin sleeps frágiles.
- H2 cubre el camino de persistencia idéntico al de producción.

---

## 10. Eliminación del código legacy

### Decisión

Eliminar `MatchTimeoutScheduler`, `CupTimeoutScheduler`, `LeagueTimeoutScheduler`,
`RematchSessionExpirationScheduler` y `SocialInvitationExpirationScheduler`. Eliminar las
propiedades `truco.<entidad>.timeout-check-interval-ms` y `truco.rematch.scheduler-delay`
asociadas. Mantener las propiedades de duración del timeout en sí
(`truco.match.idle-timeout-seconds`, etc.) — la spec aclara que el plazo no cambia.

### Rationale

- FR-013 exige eliminar el mecanismo basado en jobs periódicos.
- Las propiedades de duración permanecen como única forma configurable del plazo (FR-011).
