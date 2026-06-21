# Fase de timeout: derivar del MatchStatus, no del tipo de evento

> Decisión de dominio no obvia. Verificá contra el estado actual del código antes de asumir que
> sigue vigente.

La fase de timeout de un match (`LOBBY` = lobby-timeout-seconds, `PLAY` = play-timeout-seconds,
`NONE`) DEBE derivarse del `MatchStatus` real, estampado en `Match.getMatchDomainEvents()`, no del
tipo de `DomainEvent`.

**Por qué:** el commit `e2b4bfe` introdujo `MatchTimeoutPhasePolicy` con dos mapeos:
`phaseOf(MatchStatus)` (usado por la reconciliación) y `phaseOf(DomainEventBase)` (usado por
`MatchTimeoutEventHandler`). El segundo adivinaba la fase por el tipo de evento, y un mismo tipo
puede emitirse en distintas fases (p. ej. `PublicMatchLobbyOpenedEvent` quedaba como default → PLAY,
dándole 30 s a un lobby que debía durar 300 s; y eventos de lobby emitidos en la misma tx que
arranca la partida). Los dos mapeos divergían: el camino de eventos y el de reconciliación daban
resultados distintos.

**Cómo aplicar:** los eventos se estampan con `setMatchStatus(this.status)` en
`Match.getMatchDomainEvents()` (punto único de drenado, ya con status final). El handler mapea con
`phaseOf(event.getMatchStatus())`. NO agregar un `phaseOf(event-type)`. Si agregás un nuevo evento
de
match, no necesitás tocar el mapeo de timeout.
