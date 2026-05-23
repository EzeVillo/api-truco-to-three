# Data Model: Quick Match

**Branch**: `002-quick-match` | **Date**: 2026-05-19

---

## Domain Model

### QuickMatchTicket

**Package**: `com.villo.truco.domain.model.quickmatch`  
**Type**: Simple domain class (not `AggregateBase` — no complex state machine or domain events)

```
QuickMatchTicket
  playerId       : PlayerId        — the queued player
  gamesToPlay    : GamesToPlay     — matchmaking key
  enqueuedAt     : Instant         — used for FIFO ordering and diagnostics
```

No state transitions. Lifecycle: created on enqueue → removed on dequeue (cancel, match found, or
disconnect). The ticket is immutable once created.

**Validation rules** (enforced at creation):

- `playerId` must not be null
- `gamesToPlay` must be a valid value (enforced by GamesToPlay value object)
- `enqueuedAt` defaults to `Instant.now()` at creation

---

### QuickMatchQueue (infrastructure concept — no domain class)

The queue itself is not a domain aggregate. It is managed by `InMemoryQuickMatchQueueAdapter`
(infrastructure layer). The domain interacts with the queue only through the `QuickMatchQueuePort`
output port.

---

## Domain Port (Output Port)

### QuickMatchQueuePort

**Package**: `com.villo.truco.domain.ports`  
**Type**: Interface (domain output port)

```
interface QuickMatchQueuePort

  enqueue(ticket: QuickMatchTicket): void
    — Adds the ticket to the queue for ticket.gamesToPlay.
    — Precondition: caller verified player is not already in queue (idempotency handled above).

  tryDequeue(playerId: PlayerId): Optional<QuickMatchTicket>
    — Removes and returns the player's ticket if present, empty otherwise.
    — Used for player-initiated cancel.

  tryMatchOpponent(enqueuingPlayer: PlayerId, gamesToPlay: GamesToPlay): Optional<QuickMatchTicket>
    — Atomically finds and removes the earliest-queued ticket for gamesToPlay
      that does NOT belong to enqueuingPlayer.
    — Returns empty if no compatible opponent exists.
    — Thread-safe: concurrent calls for the same gamesToPlay are serialised.

  isPlayerQueued(playerId: PlayerId): boolean
    — Returns true if a ticket for this player exists in any gamesToPlay queue.
    — Used by PlayerAvailabilityChecker.ensureAvailable().
    
  tryDequeueBySessionId(sessionId: String): Optional<QuickMatchTicket>
    — Removes and returns the ticket for the player whose WebSocket sessionId matches.
    — Used by the disconnect event listener.
    — Note: sessionId is stored at enqueue time (passed via EnqueueForQuickMatchCommand).
```

---

## Application DTOs

### QuickMatchSearchDTO

**Package**: `com.villo.truco.application.dto`  
**Type**: Record or class — response to the enqueue endpoint

```
QuickMatchSearchDTO
  status         : QuickMatchStatus   — SEARCHING | MATCHED
  matchId        : UUID               — present only when status=MATCHED, null when SEARCHING
  enqueuedAt     : Instant            — when the player entered the queue
```

```
QuickMatchStatus (enum)
  SEARCHING  — player is in queue, no opponent yet
  MATCHED    — opponent found, match created; matchId is populated
```

---

## Infrastructure — Queue Storage Layout

```
InMemoryQuickMatchQueueAdapter (@Component)

  queues   : ConcurrentHashMap<GamesToPlay, ConcurrentLinkedDeque<QuickMatchTicket>>
               — per-gamesToPlay FIFO queue

  byPlayer : ConcurrentHashMap<PlayerId, QuickMatchTicket>
               — secondary index for O(1) isPlayerQueued / tryDequeue

  bySessId : ConcurrentHashMap<String, PlayerId>
               — secondary index for disconnect lookup (sessionId → playerId)
```

`tryMatchOpponent()` synchronizes on the per-gamesToPlay `ConcurrentLinkedDeque` to make
find-and-remove atomic. `byPlayer` and `bySessId` updates are also done inside the same
synchronized block.

---

## Match Domain Extension

### New factory method on Match aggregate

```
Match.quickMatch(
  playerOne   : PlayerId,
  playerTwo   : PlayerId,
  gamesToPlay : GamesToPlay
): Match
```

Creates a match with:

- Both players assigned immediately
- `Visibility.PRIVATE` — not broadcast to public lobby
- Status transitions directly to `IN_PROGRESS` (calls `startInternal()`)
- Publishes `MatchStartedEvent` so existing WebSocket notification pipeline delivers
  the match ID to both players on `/user/queue/match`

The factory does NOT publish `PublicMatchLobbyOpenedEvent` — the match never enters the public
lobby.

---

## PlayerAvailabilityChecker — change summary

New dependency: `QuickMatchQueuePort`  
New check in `ensureAvailable()` (after rematch session check):

```
if (quickMatchQueuePort.isPlayerQueued(playerId)) {
    throw new PlayerAlreadyInQueueException();
}
```

New exception: `PlayerAlreadyInQueueException` in
`com.villo.truco.domain.model.quickmatch.exceptions`

---

## Relationships

```
EnqueueForQuickMatchCommand
  └── EnqueueForQuickMatchCommandHandler
        ├── PlayerAvailabilityChecker.ensureAvailable()
        ├── QuickMatchQueuePort.isPlayerQueued()      [idempotency check]
        ├── QuickMatchQueuePort.tryMatchOpponent()    [attempt pairing]
        │     ├── if matched → Match.quickMatch() → MatchRepository.save()
        │     │                 → MatchEventNotifier.publishDomainEvents()
        │     │                   → /user/queue/match notification to both players
        │     └── if not matched → QuickMatchQueuePort.enqueue()
        └── returns QuickMatchSearchDTO

CancelQuickMatchSearchCommand
  └── CancelQuickMatchSearchCommandHandler
        └── QuickMatchQueuePort.tryDequeue()

SessionDisconnectEvent
  └── QuickMatchSessionDisconnectEventListener
        └── QuickMatchQueuePort.tryDequeueBySessionId()
```
