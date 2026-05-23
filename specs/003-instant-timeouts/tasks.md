# Tasks: Timeouts instantáneos por entidad

**Input**: Design documents from `/specs/003-instant-timeouts/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidos por requerimiento explícito de la spec (SC-006 + Principio III, cobertura
≥70 %) y porque `quickstart.md` enumera suites de tests específicas a producir. Títulos de tests
en español.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes).
- **[Story]**: A qué user story pertenece (US1, US2, US3).
- Cada tarea incluye ruta(s) de archivo concreta(s).

---

## Phase 1: Setup (Infraestructura compartida)

**Propósito**: Bean del scheduler, propiedades de configuración y skeleton común que todas las
historias necesitan.

- [X] T001 Agregar propiedades `truco.timeout.scheduler.pool-size` (default 4) y
  `truco.timeout.safety-net-interval` (default 5m) en `src/main/resources/application.yaml`
  (sección `truco:`).
- [X] T002 [P] Crear `TaskSchedulerConfiguration` con bean `ThreadPoolTaskScheduler`
  (poolSize configurable, `awaitTerminationSeconds=30`, `waitForTasksToCompleteOnShutdown=true`,
  thread name prefix `truco-timeout-`) en
  `src/main/java/com/villo/truco/infrastructure/config/TaskSchedulerConfiguration.java`.
- [X] T003 [P] Crear properties record `TimeoutSchedulerProperties` (poolSize, safetyNetInterval)
  en `src/main/java/com/villo/truco/infrastructure/config/TimeoutSchedulerProperties.java`.

---

## Phase 2: Foundational (Bloquea todas las historias)

**Propósito**: Puerto, value objects, implementación del scheduler in-memory, métricas, logs,
event handler dispatcher y reconciliación. Sin esto ninguna entidad puede tener timeout exacto.

**⚠️ CRITICAL**: Ninguna historia de usuario puede comenzar hasta que esta fase esté completa.

### Dominio compartido (puerto y value objects)

- [X] T004 [P] Crear enum `EntityType` con valores `MATCH`, `CUP`, `LEAGUE`, `REMATCH_SESSION`,
  `RESOURCE_INVITATION` en
  `src/main/java/com/villo/truco/application/ports/out/timeout/EntityType.java`.
- [X] T005 [P] Crear value object inmutable `TimeoutKey(EntityType type, String entityId)` con
  `equals`/`hashCode` en
  `src/main/java/com/villo/truco/application/ports/out/timeout/TimeoutKey.java`.
- [X] T006 Crear interfaz `TimeoutScheduler` con `schedule(TimeoutKey, Instant, Runnable)`,
  `cancel(TimeoutKey)`, `isPending(TimeoutKey)` en
  `src/main/java/com/villo/truco/application/ports/out/timeout/TimeoutScheduler.java`
  (depende de T004, T005).

### Implementación de infraestructura

- [X] T007 Implementar `SpringTimeoutScheduler` con
  `ConcurrentHashMap<TimeoutKey, ScheduledFuture<?>>`
  usando `compute`/`computeIfAbsent` para `schedule`/`cancel`/`reschedule` atómicos; ejecuta
  inmediatamente si `deadline <= now()` vía `taskScheduler.schedule(action, now)`; usa
  `Clock` inyectado para tests; emite logs INFO en español según contrato observabilidad, en
  `src/main/java/com/villo/truco/infrastructure/scheduler/SpringTimeoutScheduler.java`
  (depende de T002, T006).
- [X] T008 [P] Crear `TimeoutMetrics` que registra counters `truco.timeout.scheduled`,
  `truco.timeout.cancelled`, `truco.timeout.fired` (tag outcome `applied`/`skipped`),
  distribution summary `truco.timeout.lag` (ms), gauge `truco.timeout.pending`, timer
  `truco.timeout.reconcile.duration` en
  `src/main/java/com/villo/truco/infrastructure/scheduler/TimeoutMetrics.java`.
- [X] T009 Cablear `TimeoutMetrics` dentro de `SpringTimeoutScheduler` (incremento de counters y
  observación de `lag` al disparar) en
  `src/main/java/com/villo/truco/infrastructure/scheduler/SpringTimeoutScheduler.java`
  (depende de T007, T008).

### Dispatcher genérico de acciones de timeout

- [X] T010 Crear `TimeoutActionDispatcher` que dado un `TimeoutKey` resuelve el
  `*UseCase.handle(id)`
  correcto y lo envuelve en `RetryableTransactionalRunner`; registra outcome `applied`/`skipped`
  en métrica `truco.timeout.fired`; ubicado en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutActionDispatcher.java`.

### Event handler genérico

- [X] T011 Crear `TimeoutSchedulingEventHandler` (esqueleto vacío con dependencias inyectadas:
  `TimeoutScheduler`, `TimeoutActionDispatcher`, repositorios necesarios) en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`.
  Los handlers concretos por entidad se agregarán dentro de cada user story.

### Reconciliación de arranque y red de seguridad

- [X] T012 Crear interfaz `TimeoutReconciliationSource` con
  `Stream<TimeoutEntry> activeWithDeadline()`
  donde `TimeoutEntry` agrupa `TimeoutKey` + `Instant deadline` + `Runnable action`, en
  `src/main/java/com/villo/truco/application/ports/out/timeout/TimeoutReconciliationSource.java`.
  Las implementaciones por entidad se agregan en cada historia.
- [X] T013 Crear `TimeoutReconciliationRunner` que implementa
  `ApplicationListener<ApplicationReadyEvent>`: itera todos los `TimeoutReconciliationSource`,
  programa o dispara según `deadline vs now()`, registra log `Timeout reconciliado al arrancar`
  y métrica `truco.timeout.reconcile.duration` con tag `phase=startup`; en
  `src/main/java/com/villo/truco/infrastructure/scheduler/TimeoutReconciliationRunner.java`
  (depende de T007, T012).
- [X] T014 Crear `TimeoutSafetyNetScheduler` con
  `@Scheduled(fixedDelayString="${truco.timeout.safety-net-interval}")`
  que reutiliza la misma rutina de `TimeoutReconciliationRunner` con tag `phase=safety-net`; en
  `src/main/java/com/villo/truco/infrastructure/scheduler/TimeoutSafetyNetScheduler.java`
  (depende de T013).

### Health indicator

- [X] T015 Adaptar `SchedulerHeartbeatRegistry` y `SchedulerHeartbeatHealthIndicator` (o renombrar)
  para reflejar el nuevo health: UP si pool no terminado y red de seguridad corrió hace ≤10 min;
  payload `pendingByType`, `lastReconciliationAt`, `lastFireAt`; en
  `src/main/java/com/villo/truco/infrastructure/actuator/health/SchedulerHeartbeatRegistry.java`
  y
  `src/main/java/com/villo/truco/infrastructure/actuator/health/SchedulerHeartbeatHealthIndicator.java`
  (depende de T013).

### Tests del foundational

- [X] T016 [P] Test unitario `SpringTimeoutSchedulerTest` ("debe programar action para deadline
  futuro", "debe ejecutar inmediatamente si deadline ya pasó", "debe cancelar future previo al
  reprogramar misma key", "debe ser no-op al cancelar key inexistente", "isPending refleja
  estado correcto") usando `TaskScheduler` y `Clock` falsos en
  `src/test/java/com/villo/truco/infrastructure/scheduler/SpringTimeoutSchedulerTest.java`.
- [X] T017 [P] Test unitario `TimeoutKeyTest` (igualdad por valor, no por referencia) en
  `src/test/java/com/villo/truco/application/ports/out/timeout/TimeoutKeyTest.java`.

**Checkpoint**: Puerto + scheduler in-memory + reconciliación + red de seguridad + métricas listos.
Las historias pueden conectar sus entidades.

---

## Phase 3: User Story 1 — Timeout exacto de partida inactiva (Priority: P1) 🎯 MVP

**Goal**: Las partidas (`Match`) cierran por inactividad exactamente al cumplirse el plazo
(±1 s), reprograman su timeout ante cualquier acción válida y lo cancelan al finalizar normalmente.

**Independent Test**: Iniciar una partida con `truco.match.idle-timeout-seconds=10`, dejarla
inactiva 10 s y verificar (vía logs, métrica `truco.timeout.lag` y estado terminal del agregado)
que el cierre ocurre en t ∈ [9 s, 11 s] respecto a la última acción.

### Dominio Match

- [X] T018 [P] [US1] Agregar método `Instant getTimeoutDeadline(Duration idleTimeout)` que devuelve
  `lastActivityAt + idleTimeout` en
  `src/main/java/com/villo/truco/domain/model/match/Match.java`.

### Application layer Match

- [X] T019 [US1] Extender `TimeoutIdleMatchesUseCase` con método `void handle(MatchId id)` (entrada
  single-id idempotente) en
  `src/main/java/com/villo/truco/application/ports/in/TimeoutIdleMatchesUseCase.java`.
- [X] T020 [US1] Implementar `handle(MatchId id)` en `TimeoutIdleMatchesCommandHandler`: reabre el
  match en transacción, aplica `timeoutForfeit()` sólo si sigue activo (idempotencia), publica
  eventos; en
  `src/main/java/com/villo/truco/application/usecases/commands/TimeoutIdleMatchesCommandHandler.java`.
- [X] T021 [US1] Agregar a `TimeoutSchedulingEventHandler` métodos que escuchan eventos de actividad
  de match (`MatchActionPerformed` o equivalentes existentes) y eventos de cierre natural
  (`MatchFinalized`, `MatchAbandoned`); reprograman/cancelan vía `TimeoutScheduler`; en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`
  (depende de T011, T019).
- [X] T022 [US1] Registrar resolución de acción para `EntityType.MATCH` en `TimeoutActionDispatcher`
  (invoca `TimeoutIdleMatchesUseCase.handle(MatchId.of(entityId))`) en
  `src/main/java/com/villo/truco/infrastructure/config/TimeoutEventHandlerConfiguration.java`
  (depende de T010, T020).

### Infraestructura Match (reconciliación + eliminación de legacy)

- [X] T023 [P] [US1] Agregar a `MatchRepository` (puerto saliente) método
  `Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline()` que devuelve id +
  `lastActivityAt`; en
  `src/main/java/com/villo/truco/domain/ports/MatchRepository.java` y su implementación
  JPA en
  `src/main/java/com/villo/truco/infrastructure/persistence/repositories/JpaMatchRepositoryAdapter.java`.
- [X] T024 [US1] Crear `MatchTimeoutReconciliationSource` implementando
  `TimeoutReconciliationSource`: traduce filas activas a `TimeoutEntry(TimeoutKey.match(id),
      deadline, () -> dispatcher.fire(...))`; en
  `src/main/java/com/villo/truco/infrastructure/scheduler/MatchTimeoutReconciliationSource.java`
  (depende de T012, T022, T023).
- [X] T025 [US1] Eliminar `MatchTimeoutScheduler` (clase + sus tests si existen) y la propiedad
  `truco.match.timeout-check-interval-ms` de `application.yaml`; archivo a borrar:
  `src/main/java/com/villo/truco/infrastructure/scheduler/MatchTimeoutScheduler.java`.

### Tests US1

- [X] T026 [P] [US1] Test unitario "Debe programar timeout de match al recibir evento de actividad"
  y "Debe cancelar timeout de match al finalizar la partida" en
  `src/test/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandlerMatchTest.java`.
- [X] T027 [P] [US1] Test unitario "handle(MatchId) debe ser idempotente si el match ya terminó"
  en
  `src/test/java/com/villo/truco/application/usecases/commands/TimeoutIdleMatchesCommandHandlerTest.java`.
- [X] T028 [US1] Test de integración `TimeoutExactnessMatchIT` con Awaitility: crea match real con
  deadline ~2 s, verifica que la transición ocurre en `[deadline - 100 ms, deadline + 1 s]`; en
  `src/test/java/com/villo/truco/integration/TimeoutExactnessMatchIT.java`.

**Checkpoint**: Las partidas timeoutean instantáneamente y los tests de US1 pasan. MVP entregable.

---

## Phase 4: User Story 2 — Timeout exacto en copa y liga (Priority: P2)

**Goal**: `Cup` y `League` se cierran/avanzan exactamente al vencer el plazo (±1 s), reprograman
ante actividad y cancelan al cierre natural. Patrón idéntico a US1, aplicado a dos bounded contexts.

**Independent Test**: Con `truco.cup.idle-timeout-seconds=15` y
`truco.league.idle-timeout-seconds=15`,
forzar una copa y una liga al borde del plazo y verificar que cada transición automática ocurre en
t ∈ [vencimiento − 100 ms, vencimiento + 1 s].

### Cup

- [X] T029 [P] [US2] Agregar `Instant getTimeoutDeadline(Duration idleTimeout)` en
  `src/main/java/com/villo/truco/domain/model/cup/Cup.java`.
- [X] T030 [US2] Extender `TimeoutIdleCupsUseCase` con `void handle(CupId id)` en
  `src/main/java/com/villo/truco/application/ports/in/TimeoutIdleCupsUseCase.java`.
- [X] T031 [US2] Implementar `handle(CupId id)` idempotente en
  `src/main/java/com/villo/truco/application/usecases/commands/TimeoutIdleCupsCommandHandler.java`.
- [X] T032 [US2] Conectar eventos de actividad y cierre de Cup (avance de cruce, registro de
  resultado / `CupFinished`, `CupCancelled`) en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`.
- [X] T033 [US2] Registrar resolución para `EntityType.CUP` en `TimeoutActionDispatcher` en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutActionDispatcher.java`.
- [X] T034 [P] [US2] Agregar `findActiveWithTimeoutDeadline()` al repositorio de Cup en
  `src/main/java/com/villo/truco/domain/model/cup/CupRepository.java` + implementación JPA
  en `src/main/java/com/villo/truco/infrastructure/persistence/cup/JpaCupRepository.java`.
- [X] T035 [US2] Crear `CupTimeoutReconciliationSource` en
  `src/main/java/com/villo/truco/infrastructure/scheduler/CupTimeoutReconciliationSource.java`.
- [X] T036 [US2] Eliminar `CupTimeoutScheduler` y propiedad
  `truco.cup.timeout-check-interval-ms`; archivo a borrar:
  `src/main/java/com/villo/truco/infrastructure/scheduler/CupTimeoutScheduler.java`.

### League

- [X] T037 [P] [US2] Agregar `Instant getTimeoutDeadline(Duration idleTimeout)` en
  `src/main/java/com/villo/truco/domain/model/league/League.java`.
- [X] T038 [US2] Extender `TimeoutIdleLeaguesUseCase` con `void handle(LeagueId id)` en
  `src/main/java/com/villo/truco/application/ports/in/TimeoutIdleLeaguesUseCase.java`.
- [X] T039 [US2] Implementar `handle(LeagueId id)` idempotente en
  `src/main/java/com/villo/truco/application/usecases/commands/TimeoutIdleLeaguesCommandHandler.java`.
- [X] T040 [US2] Conectar eventos de actividad y cierre de League en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`.
- [X] T041 [US2] Registrar resolución para `EntityType.LEAGUE` en `TimeoutActionDispatcher` en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutActionDispatcher.java`.
- [X] T042 [P] [US2] Agregar `findActiveWithTimeoutDeadline()` al repositorio de League en
  `src/main/java/com/villo/truco/domain/model/league/LeagueRepository.java` + implementación JPA
  en `src/main/java/com/villo/truco/infrastructure/persistence/league/JpaLeagueRepository.java`.
- [X] T043 [US2] Crear `LeagueTimeoutReconciliationSource` en
  `src/main/java/com/villo/truco/infrastructure/scheduler/LeagueTimeoutReconciliationSource.java`.
- [X] T044 [US2] Eliminar `LeagueTimeoutScheduler` y propiedad
  `truco.league.timeout-check-interval-ms`; archivo a borrar:
  `src/main/java/com/villo/truco/infrastructure/scheduler/LeagueTimeoutScheduler.java`.

### Tests US2

- [X] T045 [P] [US2] Test unitario "Debe programar/cancelar timeout de cup ante eventos" en
  `src/test/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandlerCupTest.java`.
- [X] T046 [P] [US2] Test unitario "Debe programar/cancelar timeout de league ante eventos" en
  `src/test/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandlerLeagueTest.java`.
- [X] T047 [P] [US2] Test de integración `TimeoutExactnessCupIT` con Awaitility en
  `src/test/java/com/villo/truco/integration/TimeoutExactnessCupIT.java`.
- [X] T048 [P] [US2] Test de integración `TimeoutExactnessLeagueIT` con Awaitility en
  `src/test/java/com/villo/truco/integration/TimeoutExactnessLeagueIT.java`.

**Checkpoint**: Copa y liga también timeoutean instantáneamente. Tests de US2 pasan junto con US1.

---

## Phase 5: User Story 3 — Timeout exacto en invitación social y rematch (Priority: P3)

**Goal**: Invitaciones sociales (`ResourceInvitation`) y sesiones de rematch (`RematchSession`)
expiran exactamente en el instante configurado (±1 s) y cancelan ante
aceptación/rechazo/cancelación.

**Independent Test**: Crear una invitación social y una sesión de rematch ambas con expiración
~5 s en el futuro; verificar que el cambio de estado a "expirado" ocurre en
t ∈ [vencimiento − 100 ms,
vencimiento + 1 s] y que, si todos aceptan antes, no se dispara.

### Rematch Session

- [X] T049 [P] [US3] Crear `ExpireRematchSessionUseCase` con `void handle(RematchSessionId id)` en
  `src/main/java/com/villo/truco/application/ports/in/ExpireRematchSessionUseCase.java`.
- [X] T050 [US3] Implementar handler idempotente
  `ExpireRematchSessionCommandHandler` (sólo expira si status PENDING) en
  `src/main/java/com/villo/truco/application/usecases/commands/ExpireRematchSessionCommandHandler.java`.
- [X] T051 [US3] Conectar eventos en `TimeoutSchedulingEventHandler`: programar al crear sesión,
  cancelar en `RematchSessionAccepted`, `RematchSessionDeclined`, `RematchSessionCancelled`; en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`.
- [X] T052 [US3] Registrar resolución para `EntityType.REMATCH_SESSION` en
  `TimeoutActionDispatcher` en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutActionDispatcher.java`.
- [X] T053 [P] [US3] Agregar `findActiveWithExpiration()` al repositorio de RematchSession
  (puerto + impl JPA) en
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSessionRepository.java` y
  `src/main/java/com/villo/truco/infrastructure/persistence/rematch/JpaRematchSessionRepository.java`.
- [X] T054 [US3] Crear `RematchSessionTimeoutReconciliationSource` en
  `src/main/java/com/villo/truco/infrastructure/scheduler/RematchSessionTimeoutReconciliationSource.java`.
- [X] T055 [US3] Eliminar `RematchSessionExpirationScheduler` y propiedad
  `truco.rematch.scheduler-delay`; archivo a borrar:
  `src/main/java/com/villo/truco/infrastructure/scheduler/RematchSessionExpirationScheduler.java`.

### Resource Invitation (social)

- [X] T056 [P] [US3] Crear `ExpireResourceInvitationUseCase` con
  `void handle(ResourceInvitationId id)` en
  `src/main/java/com/villo/truco/social/application/ports/in/ExpireResourceInvitationUseCase.java`.
- [X] T057 [US3] Implementar handler idempotente
  `ExpireResourceInvitationCommandHandler` (sólo expira si status PENDING) en
  `src/main/java/com/villo/truco/social/application/usecases/commands/ExpireResourceInvitationCommandHandler.java`.
- [X] T058 [US3] Conectar eventos en `TimeoutSchedulingEventHandler`: programar al crear invitación,
  cancelar en `InvitationAccepted`/`InvitationDeclined`/`InvitationCancelled`; en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandler.java`
  (o `src/main/java/com/villo/truco/social/application/eventhandlers/` si el bounded context
  social lo prefiere).
- [X] T059 [US3] Registrar resolución para `EntityType.RESOURCE_INVITATION` en
  `TimeoutActionDispatcher` en
  `src/main/java/com/villo/truco/application/eventhandlers/TimeoutActionDispatcher.java`.
- [X] T060 [P] [US3] Agregar `findActiveWithExpiration()` al repositorio de ResourceInvitation
  (puerto + impl JPA) en
  `src/main/java/com/villo/truco/social/domain/ports/ResourceInvitationRepository.java` y la
  implementación JPA correspondiente bajo
  `src/main/java/com/villo/truco/social/infrastructure/persistence/`.
- [X] T061 [US3] Crear `ResourceInvitationTimeoutReconciliationSource` en
  `src/main/java/com/villo/truco/social/infrastructure/scheduler/ResourceInvitationTimeoutReconciliationSource.java`.
- [X] T062 [US3] Eliminar `SocialInvitationExpirationScheduler` y propiedad
  `truco.social.invitation-expiration-check-interval-ms` (si existe); archivo a borrar:
  `src/main/java/com/villo/truco/social/infrastructure/scheduler/SocialInvitationExpirationScheduler.java`.

### Tests US3

- [X] T063 [P] [US3] Test unitario "Debe programar timeout de rematch al crear sesión y cancelarlo
  al aceptar" en
  `src/test/java/com/villo/truco/application/eventhandlers/TimeoutSchedulingEventHandlerRematchTest.java`.
- [X] T064 [P] [US3] Test unitario "Debe programar timeout de invitación al crearla y cancelarlo al
  aceptar/declinar/cancelar" en
  `src/test/java/com/villo/truco/social/application/eventhandlers/TimeoutSchedulingEventHandlerInvitationTest.java`.
- [X] T065 [P] [US3] Test de integración `TimeoutExactnessRematchIT` con Awaitility en
  `src/test/java/com/villo/truco/integration/TimeoutExactnessRematchIT.java`.
- [X] T066 [P] [US3] Test de integración `TimeoutExactnessInvitationIT` con Awaitility en
  `src/test/java/com/villo/truco/integration/TimeoutExactnessInvitationIT.java`.

**Checkpoint**: Las cinco entidades sujetas a timeout (match, cup, league, rematch, invitación
social) timeoutean en el instante exacto. Toda la spec queda cubierta.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Propósito**: Reconciliación end-to-end, concurrencia, ArchUnit, observabilidad final,
documentación.

- [X] T067 Test de integración `TimeoutReconciliationIT`: siembra entidades activas con deadline
  ya vencido y otras futuras antes de cargar el contexto Spring; verifica que las vencidas se
  procesan dentro de 10 s del arranque y las futuras quedan agendadas en
  `src/test/java/com/villo/truco/integration/TimeoutReconciliationIT.java`.
- [X] T068 Test de concurrencia/idempotencia: dispara `handle(id)` dos veces en paralelo y verifica
  que sólo se emite un evento de fin (un test por entidad o uno parametrizado) en
  `src/test/java/com/villo/truco/application/usecases/commands/TimeoutIdempotencyTest.java`.
- [X] T069 Test de la red de seguridad: avanzar `Clock` y disparar manualmente
  `TimeoutSafetyNetScheduler.run()`; verificar que reprograma futures perdidos sin duplicar
  ejecuciones, en
  `src/test/java/com/villo/truco/infrastructure/scheduler/TimeoutSafetyNetSchedulerTest.java`.
- [X] T070 [P] Test de ArchUnit que verifica que `TimeoutScheduler` está en
  `application/ports/out/timeout` y que `domain` no importa nada del paquete `scheduler` ni de
  `application/ports/out/timeout`; agregar en
  `src/test/java/com/villo/truco/architecture/CleanArchitectureTest.java`.
- [X] T071 [P] Test del health indicator (`SchedulerHeartbeatHealthIndicatorTest`): UP si red de
  seguridad corrió hace ≤10 min, DOWN si no, en
  `src/test/java/com/villo/truco/infrastructure/actuator/health/SchedulerHeartbeatHealthIndicatorTest.java`.
- [X] T072 [P] Actualizar `README.md`: reemplazar cualquier mención al comportamiento basado en
  jobs periódicos (intervalos, schedulers eliminados) por la descripción del nuevo mecanismo
  (`TimeoutScheduler` + reconciliación + red de seguridad cada 5 min).
- [X] T073 [P] Actualizar `docs/CONTRATOS_API.md`: revisar y corregir afirmaciones sobre latencia
  de eventos WebSocket asociados a timeouts (partida, copa, liga, invitaciones, rematch);
  eliminar referencias a "hasta N segundos de retraso por el job de barrido".
- [X] T074 Ejecutar `./gradlew build` (incluye JaCoCo ≥70 % y ArchUnit) y verificar que pasa.
- [ ] T075 Ejecutar manualmente los Casos 1–5 de `specs/003-instant-timeouts/quickstart.md` con la
  app corriendo localmente y registrar evidencia (logs + métricas) en el PR.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias. T002 y T003 en paralelo; T001 independiente.
- **Foundational (Phase 2)**: Depende de Phase 1. T004/T005 en paralelo, luego T006. T007 depende
  de T002+T006. T008/T010/T011/T012 paralelizables tras sus deps. T013 depende de T007+T012. T014
  depende de T013. T015 depende de T013. Tests T016/T017 en paralelo al final.
- **User Stories (Phase 3–5)**: Cada historia depende de Foundational completo. Una vez completo,
  US1/US2/US3 pueden trabajarse en paralelo por distintos desarrolladores; los archivos son
  independientes excepto por `TimeoutSchedulingEventHandler.java` y `TimeoutActionDispatcher.java`,
  que son punto de coordinación (no [P] entre historias para esos archivos).
- **Polish (Phase 6)**: Depende de las tres historias completas.

### Dependencias dentro de cada historia

- Test → Modelo de dominio → UseCase port → Handler → EventHandler conexión → Dispatcher resolution
  → Repository extension → ReconciliationSource → Borrado de scheduler legacy.

### Parallel Opportunities

- **Phase 1**: T002 ∥ T003.
- **Phase 2**: T004 ∥ T005 (luego T006); T008 ∥ T010 ∥ T011 ∥ T012 (tras T007); T016 ∥ T017.
- **Phase 3 (US1)**: T018 ∥ T023; T026 ∥ T027 ∥ T028 (distintos archivos de test).
- **Phase 4 (US2)**: Bloques Cup y League casi independientes entre sí: T029/T034 ∥ T037/T042;
  los handlers comparten archivo (`TimeoutSchedulingEventHandler` y `TimeoutActionDispatcher`),
  por eso T032/T040 y T033/T041 deben secuenciarse.
- **Phase 5 (US3)**: Bloques rematch y resource invitation paralelos salvo en archivos compartidos;
  T063 ∥ T064 ∥ T065 ∥ T066.
- **Phase 6**: T070 ∥ T071 ∥ T072 ∥ T073.

---

## Parallel Example: Phase 2 Foundational (tras T007)

```
Task: "Implementar TimeoutMetrics en infrastructure/scheduler/TimeoutMetrics.java" (T008)
Task: "Crear TimeoutActionDispatcher en application/eventhandlers/TimeoutActionDispatcher.java" (T010)
Task: "Crear TimeoutSchedulingEventHandler skeleton en application/eventhandlers/TimeoutSchedulingEventHandler.java" (T011)
Task: "Crear interfaz TimeoutReconciliationSource en application/ports/out/timeout/TimeoutReconciliationSource.java" (T012)
```

## Parallel Example: User Story 1 (tras T020)

```
Task: "Agregar getTimeoutDeadline a Match.java" (T018)
Task: "Agregar findActiveWithTimeoutDeadline a MatchRepository (puerto + JPA)" (T023)
Task: "Test 'Debe programar timeout de match al recibir evento de actividad'" (T026)
Task: "Test 'handle(MatchId) idempotente'" (T027)
Task: "Test de integración TimeoutExactnessMatchIT" (T028)
```

---

## Implementation Strategy

### MVP First (US1 solamente)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational (CRÍTICO — bloquea las tres historias).
3. Completar Phase 3: US1 (match).
4. **STOP and VALIDATE**: ejecutar Caso 1 de `quickstart.md` y `TimeoutExactnessMatchIT`.
5. Deploy/demo. La entidad de mayor frecuencia ya está cubierta y entrega el mayor valor percibido.

### Entrega incremental

1. Setup + Foundational → fundación lista.
2.
    + US1 (match) → MVP entregable.
3.
    + US2 (cup + league) → torneos cubiertos.
4.
    + US3 (rematch + invitación social) → cobertura completa de la spec.
5.
    + Polish → docs actualizadas, ArchUnit verde, build con cobertura ≥70 %.

### Equipo en paralelo

Con tres desarrolladores tras Foundational:

- Dev A: US1 (match).
- Dev B: US2 (cup + league).
- Dev C: US3 (rematch + invitación social).

Punto de coordinación: `TimeoutSchedulingEventHandler.java` y `TimeoutActionDispatcher.java` reciben
contribuciones de las tres historias; coordinar merges por método/handler interno.

---

## Notes

- Tests con tiempo usan `Clock` inyectado (foundational) y `Awaitility` (integración) — nunca
  `Thread.sleep` con margen frágil.
- Idempotencia se apoya en el optimistic locking JPA existente + chequeo de estado en cada
  agregado (`if (status != ACTIVE) return;`).
- Cada borrado de scheduler legacy (T025, T036, T044, T055, T062) DEBE remover también la
  propiedad de configuración asociada en `application.yaml` para cumplir FR-013.
- Los plazos de duración (`truco.match.idle-timeout-seconds`, etc.) NO se tocan — sólo se eliminan
  los intervalos de chequeo del job legacy (FR-011 vs FR-013).
- Tras Phase 6, revisar que las dos entradas de "Documentación a actualizar" de la spec quedaron
  cumplidas.
