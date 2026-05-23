# Data Model: Timeouts instantáneos por entidad

## Visión general

Esta feature **no introduce tablas nuevas**. Reutiliza el deadline derivable de las entidades
existentes y agrega un único objeto en memoria (`TimeoutSchedule`) gestionado por el puerto
`TimeoutScheduler`. La fuente autoritativa sigue siendo la base de datos.

## Entidades y deadlines

### Match (partida)

- **Tabla**: existente.
- **Atributos relevantes ya presentes**: `lastActivityAt`, `status`, `version`.
- **Deadline derivado**: `lastActivityAt + truco.match.idle-timeout-seconds`.
- **Eventos que reinician el deadline**: cualquier acción de juego válida (carta jugada, envido
  cantado, truco aceptado, etc.) — ya emiten un evento de dominio que se traduce en update de
  `lastActivityAt`.
- **Eventos que cancelan el deadline**: `MatchFinalized`, `MatchAbandoned`, transición a estado
  terminal.
- **Acción al vencer**: `TimeoutIdleMatchesUseCase.handle(matchId)` → `match.timeoutForfeit()`.

### Cup (copa)

- **Tabla**: existente.
- **Atributos relevantes**: `lastActivityAt` o equivalente, `status`, `version`.
- **Deadline derivado**: `lastActivityAt + truco.cup.idle-timeout-seconds`.
- **Reinicia con**: avance de cruce, registro de resultado.
- **Cancela con**: `CupFinished`, `CupCancelled` (cambio a estado terminal previo al timeout).
- **Acción al vencer**: `TimeoutIdleCupsUseCase.handle(cupId)` → `cup.cancel()`.

### League (liga)

- Análoga a Cup. `lastActivityAt + truco.league.idle-timeout-seconds`.
- **Acción al vencer**: `TimeoutIdleLeaguesUseCase.handle(leagueId)` → `league.cancel()`.

### Rematch Session

- **Tabla**: existente.
- **Atributos relevantes**: `expirationAt`, `status`.
- **Deadline**: `expirationAt` directamente (no se reinicia, es un timeout único).
- **Cancela con**: `RematchSessionAccepted`, `RematchSessionDeclined`,
  `RematchSessionCancelled`.
- **Acción al vencer**: `ExpireRematchSessionUseCase.handle(sessionId)` →
  `rematchSession.expire()`.

### Resource Invitation (invitación social)

- **Tabla**: existente.
- **Atributos relevantes**: `expiresAt`, `status`.
- **Deadline**: `expiresAt` directamente.
- **Cancela con**: `InvitationAccepted`, `InvitationDeclined`, `InvitationCancelled`.
- **Acción al vencer**: `ExpireResourceInvitationUseCase.handle(invitationId)`.

## Objeto en memoria: TimeoutSchedule

```text
TimeoutSchedule
├── key: TimeoutKey      // value object: (entityType, entityId)
├── deadline: Instant    // momento de disparo
└── future: ScheduledFuture<?>
```

Mantenido por `SpringTimeoutScheduler` en `Map<TimeoutKey, TimeoutSchedule>`. No persistido.

### Estados (in-memory)

```
[ausente] --schedule--> PENDING --cancel--> [ausente]
                          |
                         fire
                          v
                       FIRED (entrada eliminada del mapa)
```

`reschedule` = `cancel` seguido de `schedule`. Atómico desde el punto de vista del consumidor.

## Cambios de schema

**Ninguno previsto** en el caso base. Si durante la implementación se descubre que alguna entidad
no expone `lastActivityAt` o equivalente, se agregará por migración Flyway con valor inicial
`= updatedAt` y se documentará en el `tasks.md`.

## Reglas de validación

- `deadline` DEBE ser un `Instant` no nulo; si es `null`, no se agenda timeout (el evento de
  dominio probablemente correspondía a una entidad ya terminada).
- Si `deadline <= now()` al programar, se ejecuta inmediatamente en el pool del scheduler.
- `cancel` sobre una clave inexistente es no-op (no error).
- `schedule` sobre una clave existente reemplaza el future previo (cancela y reagenda).

## Concurrencia y consistencia

- Toda mutación al `Map<TimeoutKey, TimeoutSchedule>` ocurre dentro de un `compute`/
  `computeIfAbsent`
  sobre `ConcurrentHashMap` para evitar carreras entre `schedule` y `cancel`.
- La ejecución del timeout abre una transacción nueva (vía `RetryableTransactionalRunner`,
  reutilizando el patrón existente) y aplica la transición sólo si el agregado todavía la
  admite. La idempotencia ya está cubierta por la lógica de los handlers actuales.
