# Research: Quick Match — Phase 0 Findings

**Branch**: `002-quick-match` | **Date**: 2026-05-19

---

## Decision 1 — Thread-safe in-memory queue structure

**Decision**: `ConcurrentHashMap<GamesToPlay, ConcurrentLinkedDeque<QuickMatchTicket>>` for the
per-gamesToPlay queues, plus a secondary `ConcurrentHashMap<PlayerId, QuickMatchTicket>` for O(1)
player lookup (isPlayerQueued, cancel, and dequeue-by-player). The "find opponent and remove them"
operation is atomic by synchronizing on the per-gamesToPlay deque instance.

**Rationale**: `ConcurrentLinkedDeque` provides thread-safe FIFO ordering without global locks. The
secondary map keeps isPlayerQueued and cancel at O(1). Synchronizing only on the individual
gamesToPlay deque (not the outer map) keeps contention minimal — players in different gamesToPlay
buckets never block each other.

**Alternatives considered**:

- Single `LinkedBlockingQueue<QuickMatchTicket>` — too coarse, no per-gamesToPlay grouping.
- Persistent DB queue — over-engineered for an ephemeral store; FR says in-memory is acceptable.

---

## Decision 2 — WebSocket disconnect → PlayerId resolution

**Decision**: In `QuickMatchSessionDisconnectEventListener`, extract the principal directly from the
`SessionDisconnectEvent` via `StompHeaderAccessor.wrap(event.getMessage()).getUser()`. The project's
`WebSocketAuthInterceptor` already sets the authenticated `JwtUser` principal on every STOMP
session, so the player identity is always available on disconnect without a separate registry.

**Rationale**: `SpectateSessionTerminationEventListener` (existing) uses a registry because
spectator subscriptions are not tied to authentication. Quick Match queue entries ARE tied to
authenticated players, so the principal is available directly. Avoids the need for a separate
`QuickMatchSessionRegistry` component.

**Alternatives considered**:

- Separate `QuickMatchSessionRegistry` (sessionId → PlayerId map, populated on enqueue) — adds
  more state to manage; unnecessary given the principal is already in the WebSocket session.

---

## Decision 3 — FR-011 bypass (system-created match)

**Decision**: Remove both players from the queue atomically BEFORE creating the match. Since
`PlayerAvailabilityChecker.ensureAvailable()` will include an `isPlayerQueued()` check, removing
them first ensures neither player is seen as "in queue" during match creation. No special bypass
flag or code path is needed — the ordering of operations provides the bypass naturally.

**Rationale**: Clean, no special cases. The sequence is: (1) find opponent in queue, (2) remove
opponent ticket, (3) create match for both. Step 2 makes both players "available" before step 3.

**Alternatives considered**:

- A separate `ensureAvailableForSystemAction()` method that skips the queue check — possible but
  unnecessary given the natural ordering.

---

## Decision 4 — Match creation for paired players

**Decision**: Add a new static factory `Match.quickMatch(PlayerId playerOne, PlayerId playerTwo,
GamesToPlay gamesToPlay)` to the `Match` aggregate. It creates the match with both players already
assigned, `Visibility.PRIVATE`, and transitions directly to `IN_PROGRESS` (mimicking the combined
effect of `create()` + `joinPublic()` + `startInternal()` but without exposing the match in the
public lobby). The factory publishes the same `MatchStartedEvent` that existing handlers use for
WebSocket notifications.

**Rationale**: Using `Match.create()` + an existing `joinPublic()` call would set
`Visibility.PUBLIC`
(or require public visibility for auto-start), violating FR-009. Using `createReady()` was designed
for single-player bot matches and requires a separate start step. A dedicated factory makes the
intent explicit, reuses `startInternal()`, and keeps both players private from the lobby.

**Alternatives considered**:

- `Match.create(playerOne, PRIVATE)` + new `Match.joinAsMatchedPlayer(playerTwo)` — requires a new
  join method anyway; same complexity, less self-describing.
- `Match.createReady()` + `Match.start()` — `createReady()` was not designed for two human players;
  adapting it risks breaking bot/league/cup paths.

---

## Decision 5 — WebSocket notification channel for pairing

**Decision**: Reuse the existing `/user/queue/match` personal channel and the existing
`MatchEventNotifier` infrastructure. When `Match.quickMatch()` publishes `MatchStartedEvent`, the
existing `MatchNotificationEventTranslator` + `StompMatchNotificationHandler` delivers the event
to both players on their existing match channel. No new WebSocket channel or handler needed.

**Rationale**: The existing match notification pipeline already delivers match-start events to both
players. Adding a separate quick-match channel would duplicate infrastructure for no benefit — the
client already listens on `/user/queue/match` for match updates.

**Alternatives considered**:

- New `/user/queue/quick-match` channel — unnecessary; increases client-side surface with no
  payoff.

---

## Decision 6 — Idempotency for double-entry (FR-008)

**Decision**: `EnqueueForQuickMatchCommandHandler` calls `queuePort.isPlayerQueued(playerId)` at
the start (before `ensureAvailable()`). If already queued, return a `QuickMatchSearchDTO` with
`status=SEARCHING` and the original `enqueuedAt` timestamp — no error, no duplicate ticket. This
is NOT an availability violation; it is a silent idempotent re-acknowledgment.

**Rationale**: FR-008 explicitly requires idempotency. Returning an error on double-entry would
force the client to track state; returning the current status is more robust for disconnected
clients that retry.

**Note**: `ensureAvailable()` would also throw `PlayerAlreadyInQueueException` for a second
request, so the idempotency check must run BEFORE the availability check.

---

## Decision 7 — PlayerAvailabilityChecker extension

**Decision**: Add `QuickMatchQueuePort` as a new dependency of `PlayerAvailabilityChecker`. Add a
call to `quickMatchQueuePort.isPlayerQueued(playerId)` in `ensureAvailable()` immediately after the
rematch session check (line 47). Throw `PlayerAlreadyInQueueException` on positive result.
`PlayerAvailabilityConfiguration` is updated to inject the `QuickMatchQueuePort` bean.

**Rationale**: Consistent with how `RematchSessionRepository` is integrated. All other handlers
that call `ensureAvailable()` (create match, join match, create bot match, enter league/cup) will
automatically respect the queue restriction without any changes to those handlers.

**Alternatives considered**:

- A separate checker method `ensureNotInQueue(playerId)` called selectively — would require
  modifying all callers; violates DRY.
