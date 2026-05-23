# Implementation Plan: Timeouts instantáneos por entidad

**Branch**: `003-instant-timeouts` | **Date**: 2026-05-22 |
**Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-instant-timeouts/spec.md`

## Summary

Reemplazar los cinco jobs periódicos de timeout (`MatchTimeoutScheduler`,
`CupTimeoutScheduler`, `LeagueTimeoutScheduler`, `RematchSessionExpirationScheduler` y
`SocialInvitationExpirationScheduler`) por un mecanismo de **timeouts programados por entidad** que
se disparen exactamente en el instante de vencimiento.

Enfoque técnico: cada entidad sujeta a timeout expone un *deadline* derivado de su última
actividad. Un puerto de aplicación `TimeoutScheduler` (con implementación en infraestructura
basada en `TaskScheduler` de Spring) acepta `schedule(entityType, entityId, deadline)` y
`cancel(entityType, entityId)`. Listeners de eventos de dominio reprograman/cancelan ante actividad
y cierre natural. Al arrancar el servicio, un `ApplicationReadyEvent` listener reconcilia el estado
contra la base de datos: reprograma los timeouts pendientes y dispara inmediatamente los ya
vencidos. La fuente de verdad sigue siendo la BD (deadline en columnas existentes/nuevas); el
scheduler in-memory es un mero acelerador para que el disparo ocurra en el instante exacto.

## Technical Context

**Language/Version**: Java 21 (toolchain del proyecto)

**Primary Dependencies**:

- Spring Boot (web, scheduling, data-jpa)
- Spring `TaskScheduler` (`ThreadPoolTaskScheduler`)
- Hibernate / JPA + PostgreSQL (prod), H2 modo PostgreSQL (tests)
- ArchUnit (validación de capas en build)
- JaCoCo (cobertura mínima 70 %)

**Storage**: PostgreSQL en producción; H2 en memoria para tests. Las entidades implicadas
(`Match`, `Cup`, `League`, invitación social, rematch session) ya residen en JPA; se reutilizan
columnas existentes de "última actividad / vencimiento" cuando existen, o se agregan campos
mínimos (`timeout_deadline`, `timeout_status`) sólo si no hay equivalente.

**Testing**: JUnit 5 + Mockito + Spring Boot Test (slice tests). Tests de tiempo usan `Clock`
inyectado para no depender del wall clock.

**Target Platform**: JVM, Linux server. Despliegue habitual single-instance, pero el diseño DEBE
ser correcto bajo multi-instancia (FR-012).

**Project Type**: Servicio web Spring Boot monolítico siguiendo Clean/Hexagonal + DDD
(`Domain → Application → Infrastructure`).

**Performance Goals**:

- Disparo del timeout dentro de los 1000 ms del vencimiento (SC-001, SC-002).
- Procesamiento de timeouts vencidos al arranque en ≤10 s (SC-005).
- Capacidad estimada: O(10 k) timeouts simultáneos sin degradación notable
  (`ScheduledThreadPoolExecutor` soporta esto holgadamente).

**Constraints**:

- Sin pérdida ni duplicación tras reinicios.
- Exactamente-una ejecución por entidad incluso con múltiples instancias (vía idempotencia en
  el handler de dominio, no vía lock distribuido).
- Sin Spring/JPA en `domain/`.
- Manteniendo cobertura ≥70 %.

**Scale/Scope**: Cinco timeouts cubiertos (match, cup, league, invitación social, rematch). No se
contempla un sexto en este alcance. El patrón queda lo bastante genérico como para incorporar
nuevos en el futuro sin cambios estructurales.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                          | Cumplimiento | Notas                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|------------------------------------|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| I. Arquitectura Hexagonal + DDD    | ✅            | Nuevo **puerto de entrada** `EntityTimeoutFiredUseCase` o reuso del existente `TimeoutIdle*UseCase`; nuevo **puerto de salida** `TimeoutScheduler` (interfaz en `application/ports/out`). Implementación con Spring `TaskScheduler` va en `infrastructure/scheduler`. Los handlers existentes (`TimeoutIdle*CommandHandler`) se reusarán como punto de entrada para "disparar un timeout"; se les agrega `handle(entityId)`. Sin cross-aggregate imports. |
| II. Dominio Puro                   | ✅            | El dominio no conoce el scheduler. La interfaz `TimeoutScheduler` vive en `application` (es un puerto saliente desde la perspectiva de los casos de uso, no del dominio). Domain solo expone el deadline calculado a partir de la última actividad (regla de negocio).                                                                                                                                                                                    |
| III. Test-First con coverage ≥70 % | ✅            | Plan incluye tests unitarios para `TimeoutScheduler` (con `Clock` inyectado), tests de integración con H2 que verifican exactitud temporal usando `Awaitility`, y tests para reconciliación en arranque. Títulos en español.                                                                                                                                                                                                                              |
| IV. Español como idioma de trabajo | ✅            | Spec, plan, research, data-model, contratos, quickstart, tasks y títulos de tests en español. Código sigue convenciones existentes (inglés).                                                                                                                                                                                                                                                                                                              |
| V. Simplicidad / YAGNI             | ✅            | Se descarta Quartz/cola persistente externa: la BD ya es el estado autoritativo; el scheduler in-memory es un *cache* de disparos. Sólo se agregan columnas si la entidad no tiene equivalente útil hoy. Se elimina el job periódico salvo un **reconciliador de muy baja frecuencia** (cada 5 min) como red de seguridad — justificado para FR-008 ante caídas no detectadas; ver Complexity Tracking.                                                   |

**Resultado**: Pasa. Una desviación menor — el reconciliador periódico de baja frecuencia —
documentada abajo.

## Project Structure

### Documentation (this feature)

```text
specs/003-instant-timeouts/
├── plan.md              # Este archivo
├── research.md          # Decisiones técnicas con alternativas
├── data-model.md        # Entidades, deadlines, transiciones
├── quickstart.md        # Cómo verificar la feature localmente
├── contracts/
│   ├── timeout-scheduler-port.md   # Contrato del puerto de salida
│   └── observability.md            # Métricas y health
├── checklists/
│   └── requirements.md  # ya generado en /speckit-specify
└── tasks.md             # Generado por /speckit-tasks
```

### Source Code (repository root)

Estructura existente del monolito. Cambios y ubicación de nuevos archivos:

```text
src/main/java/com/villo/truco/
├── domain/
│   ├── model/match/Match.java                          # +método getTimeoutDeadline()
│   ├── model/cup/Cup.java                              # +método getTimeoutDeadline()
│   ├── model/league/League.java                        # +método getTimeoutDeadline()
│   ├── (rematch/social) — métodos análogos
│   └── events/
│       └── (eventos existentes; se reusan para señalar actividad/cierre)
│
├── application/
│   ├── ports/
│   │   ├── out/
│   │   │   └── TimeoutScheduler.java                   # NUEVO puerto de salida
│   │   └── in/
│   │       ├── TimeoutIdleMatchesUseCase.java          # +handle(MatchId)
│   │       ├── TimeoutIdleCupsUseCase.java             # +handle(CupId)
│   │       ├── TimeoutIdleLeaguesUseCase.java          # +handle(LeagueId)
│   │       ├── ExpireRematchSessionUseCase.java        # NUEVO (o método en existente)
│   │       └── ExpireResourceInvitationUseCase.java    # NUEVO (o método en existente)
│   ├── usecases/commands/
│   │   ├── TimeoutIdleMatchesCommandHandler.java       # idempotente per-id
│   │   ├── TimeoutIdleCupsCommandHandler.java          # idempotente per-id
│   │   ├── TimeoutIdleLeaguesCommandHandler.java       # idempotente per-id
│   │   └── ...
│   └── eventhandlers/
│       └── TimeoutSchedulingEventHandler.java          # NUEVO: escucha actividad/cierre → reschedule/cancel
│
└── infrastructure/
    ├── scheduler/
    │   ├── SpringTimeoutScheduler.java                 # NUEVO: implementación TaskScheduler
    │   ├── TimeoutReconciliationRunner.java            # NUEVO: ApplicationReadyEvent listener
    │   ├── TimeoutSafetyNetScheduler.java              # NUEVO: red de seguridad cada 5 min
    │   ├── MatchTimeoutScheduler.java                  # ELIMINADO
    │   ├── CupTimeoutScheduler.java                    # ELIMINADO
    │   ├── LeagueTimeoutScheduler.java                 # ELIMINADO
    │   ├── RematchSessionExpirationScheduler.java      # ELIMINADO
    │   └── (social).SocialInvitationExpirationScheduler.java  # ELIMINADO
    ├── config/
    │   └── TaskSchedulerConfiguration.java             # NUEVO: ThreadPoolTaskScheduler bean
    └── actuator/health/
        └── SchedulerHeartbeatRegistry.java             # adaptado para nuevas métricas

src/test/java/com/villo/truco/
├── application/eventhandlers/TimeoutSchedulingEventHandlerTest.java
├── infrastructure/scheduler/SpringTimeoutSchedulerTest.java
├── infrastructure/scheduler/TimeoutReconciliationRunnerTest.java
└── integration/TimeoutExactnessIT.java                 # Awaitility, mide ±1 s real
```

**Structure Decision**: Monolito Spring Boot existente; mantenemos la división
`domain/application/infrastructure`. El puerto `TimeoutScheduler` vive en
`application/ports/out` porque su consumidor son los handlers de aplicación, no el dominio puro.

## Complexity Tracking

| Violación                                                              | Por qué se necesita                                                                                                                                                                                                                                                                                                                                                                          | Alternativa más simple, rechazada porque                                                                                                                                                                                                                                                                                                      |
|------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Mantener un reconciliador periódico (cada 5 min) como red de seguridad | FR-008 + SC-005 exigen procesar lo vencido tras caídas; un crash entre "agendar in-memory" y "no haberse disparado todavía" deja huérfano el timeout en una sola instancia. La reconciliación al arrancar cubre el caso de reinicio limpio; el reconciliador periódico cubre el caso de instancia activa que perdió el timer in-memory (p. ej. tras una excepción en el hilo del scheduler). | Confiar sólo en la reconciliación al arrancar deja una ventana en la que un timeout perdido nunca se dispara hasta el próximo reinicio. La complejidad agregada es muy baja (un `@Scheduled` cada 5 min que ejecuta la misma reconciliación) y elimina una clase de bug difícil de diagnosticar. La spec lo permite explícitamente en FR-013. |
