# Feature Specification: Quick Match (Partida Rápida)

**Feature Branch**: `002-quick-match`

**Created**: 2026-05-19

**Status**: Draft

**Input**: User description: "quiero hacer un sistema de match rapido, actualmente hay partidas
privadas, y publicas, las publicas se puede enfrentar cualquiera con cualquiera, pero hay que buscar
una partida, lo cual quiza no es lo mejor, y me gustaria tener un boton que sea para buscar una
partida rapida, y que te matchee con otro jugador que haya seleccionado lo mismo, no me importa nada
acerca del ELO/MMR por el momento"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find and Start a Quick Match (Priority: P1)

As a logged-in player, I want to press a "Quick Match" button, choose how many games I want to play
to win, and be automatically paired with another human player who made the same selection — so I can
start playing without browsing the public lobby.

**Why this priority**: This is the entire purpose of the feature. All other stories support or
refine this core flow.

**Independent Test**: A player enters the queue and a second player with the same `gamesToPlay`
enters; both are paired and the match starts. Each player receives a notification with the match
identifier. Can be tested independently with two test accounts.

**Acceptance Scenarios**:

1. **Given** player A is authenticated and not in an active match, **When** player A enters the
   Quick Match queue with `gamesToPlay=3`, **Then** player A is placed in the queue and receives a
   confirmation that the search has started.

2. **Given** player A is in the Quick Match queue with `gamesToPlay=3` and player B (different user,
   also not in an active match) enters the queue with `gamesToPlay=3`, **When** the system finds
   both players, **Then** a match between A and B is created, the match starts immediately, and both
   A and B receive a real-time notification containing the match identifier.

3. **Given** player A is in the queue with `gamesToPlay=3` and player B enters with `gamesToPlay=6`,
   **When** the system evaluates the queue, **Then** A and B are NOT paired; each remains waiting in
   their respective queues independently.

---

### User Story 2 - Cancel Quick Match Search (Priority: P2)

As a logged-in player who is waiting in the Quick Match queue, I want to cancel my search so I can
do something else (e.g., join a friend's match or create a private game) without being stuck
waiting.

**Why this priority**: Without cancellation, the queue is a trap. Players must be able to leave
voluntarily. This is required for a usable feature.

**Independent Test**: A player enters the queue, then cancels. The player is removed from the queue,
receives confirmation, and can immediately start another action (e.g., enter the queue again).

**Acceptance Scenarios**:

1. **Given** player A is in the Quick Match queue, **When** player A requests to cancel the search,
   **Then** player A is removed from the queue and receives confirmation that the search was
   cancelled.

2. **Given** player A has cancelled their Quick Match search, **When** player B with the same
   `gamesToPlay` enters the queue, **Then** player B is NOT paired with player A (player A is no
   longer in the queue).

---

### User Story 3 - Automatic Queue Cleanup on Disconnection (Priority: P3)

As the system, when a player who is waiting in the Quick Match queue disconnects (e.g., closes the
browser or loses connectivity), I want that player removed from the queue automatically — so no "
ghost" entries block future matchmaking.

**Why this priority**: Ghost queue entries would degrade matchmaking quality over time and create
confusing states. Important for correctness but requires the core matchmaking to exist first.

**Independent Test**: A player enters the queue and then their WebSocket connection is closed
server-side. Inspecting the queue state confirms the player is no longer present. A subsequent
player with the same `gamesToPlay` finds no phantom match.

**Acceptance Scenarios**:

1. **Given** player A is in the Quick Match queue, **When** player A's real-time connection is
   closed (disconnection event), **Then** player A is automatically removed from the queue.

2. **Given** player A was removed from the queue due to disconnection, **When** player A reconnects
   and enters the queue again, **Then** player A joins the queue as a fresh entry with no residual
   state.

---

### Edge Cases

- **Double entry**: A player who is already in the queue tries to enter again — the system must
  treat this as idempotent (no duplicate entry; existing position is preserved and returned).
- **Match already formed (race condition on cancel)**: A player requests to cancel just as they are
  being paired. The pairing sequence atomically removes the player's ticket from the queue before
  creating the match (FR-011). A cancel request arriving after this point finds no ticket and
  completes silently as a no-op. The match proceeds and the player is notified of the match via
  FR-005. This is not a violation of FR-002: FR-002 guarantees removal from the queue, which has
  already occurred; the cancel simply has nothing left to remove.
- **Already in active match**: A player with an existing active match (not finished/cancelled)
  attempts to enter the Quick Match queue — the system rejects the request with a clear error.
- **Three players arrive simultaneously**: Players A, B, and C all enter the queue with the same
  `gamesToPlay` at nearly the same time — A and B are paired (first-come-first-served), C remains
  in the queue. C receives no notification at this point; C will only be notified when a future
  opponent joins the queue with the same `gamesToPlay`.
- **Server restart**: The in-memory queue is cleared. Players who were waiting receive no explicit
  notification (reconnect behavior is out of scope for v1). Accepted as a known limitation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow an authenticated player to join a Quick Match queue by
  specifying a `gamesToPlay` value. The confirmation response MUST include the search status
  (`SEARCHING` or `MATCHED`), the enqueue timestamp, and — when matched — the match identifier.
- **FR-002**: The system MUST allow an authenticated player to cancel their Quick Match search and
  be removed from the queue. The cancellation confirmation MUST be an empty success response (no
  body required).
- **FR-003**: The system MUST pair the two earliest-queued players who share the same `gamesToPlay`
  value (FIFO order).
- **FR-004**: When two players are paired, the system MUST start a match between them immediately
  (no additional ready-up step required from the players; "immediately" is bounded by SC-001).
- **FR-005**: When a match is created via Quick Match, BOTH players MUST receive a real-time
  notification containing the match identifier so they can navigate to the match.
- **FR-006**: The system MUST reject a player's attempt to join the Quick Match queue if that player
  already has an active match in progress.
- **FR-007**: The system MUST remove a player from the Quick Match queue automatically when that
  player's WebSocket connection is closed (detected via a STOMP session disconnect event). Session
  token expiry and HTTP-level disconnects are out of scope for v1.
- **FR-008**: A player who is already in the Quick Match queue and submits another join request MUST
  NOT be added twice; the system treats the second request as idempotent.
- **FR-009**: Matches created through Quick Match MUST NOT appear in the public match lobby
  discovery list.
- **FR-010**: The system MUST treat a player who is in the Quick Match queue as "occupied": that
  player MUST be blocked from joining, searching, or creating any other match (public, private, bot,
  or tournament) until the queue search is cancelled or a match is created.
- **FR-011**: The system's own pairing action — creating a match for two players who matched in the
  queue — MUST bypass the "player is in Quick Match queue" availability restriction. The bypass is
  achieved by removing both players' queue tickets atomically before creating the match; at the
  moment of match creation, neither player is considered "in queue" and no restriction applies. This
  is a system-initiated sequence, not a player-initiated action.
- **FR-012**: The system MUST NOT send any intermediate status updates to a player while they are
  waiting in the queue. The only queue-related notification the player receives is the final pairing
  event (FR-005). The client is expected to display a waiting state until that event arrives.

### Key Entities

- **QuickMatchTicket**: Represents a player's slot in the matchmaking queue. Key attributes: player
  identity, chosen `gamesToPlay`, time of entry. Used to enforce FIFO ordering and deduplication.
- **QuickMatchQueue**: Groups pending `QuickMatchTicket` entries by `gamesToPlay`. The queue is a
  passive data structure; the application layer detects when two compatible tickets exist and
  triggers match creation. The queue is ephemeral — it does not survive system restarts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When a second compatible player joins the queue, both players are paired and notified
  within 2 seconds of the second player's action.
- **SC-002**: 100% of matches created via Quick Match pair only players with identical `gamesToPlay`
  values; no cross-configuration matches are created.
- **SC-003**: 0% of disconnected players remain as active entries in the queue after their
  connection is closed; ghost entries are eliminated.
- **SC-004**: A player can enter the queue, cancel, and re-enter successfully with no residual
  state — verified in 100% of such sequences.
- **SC-005**: Players already in an active match are rejected from the queue 100% of the time with a
  clear, human-readable error response.
- **SC-006**: Players who are currently in the Quick Match queue are rejected from all other
  match-creation or match-joining actions (public, private, bot, or tournament) 100% of the time
  with a clear error response, for as long as they remain in the queue.

## Assumptions

- The Quick Match queue is **ephemeral and in-memory**. It does not need to survive server restarts;
  this simplifies the initial implementation. Players who were waiting when the server restarts will
  need to re-queue manually.
- The only matchmaking criterion is `gamesToPlay`, since that is the only configurable rule for a
  match today. No other game options (e.g., with/without "flor") exist yet.
- Matches created via Quick Match reuse the same state-machine behaviour as auto-started matches
  (both players assigned, match transitions directly to the active state without a confirmation
  step). The implementation uses a dedicated factory method rather than the public-join code path —
  "reuse behaviour" refers to the outcome, not the code path.
- Quick Match matches are not exposed in the public lobby. They are system-created matches —
  discovery is not relevant because the pairing is automatic.
- The player's real-time connection is the mechanism for detecting disconnection. If a player
  disconnects and reconnects before being paired, they must re-enter the queue manually (
  reconnect-and-resume is out of scope for v1).
- No ELO, MMR, skill rating, or rank-based filtering is applied. Any two players with the same
  `gamesToPlay` may be paired regardless of their skill level.
- A player can only be in one Quick Match queue at a time. Joining a different `gamesToPlay` queue
  while already waiting in another is treated as invalid (equivalent to double-entry idempotency
  rule).

## Clarifications

### Session 2026-05-19

- Q: Should being in the Quick Match queue block the player from other match actions? → A: Yes. A
  player in the queue is considered "occupied" and must be blocked from joining, searching, or
  creating any other match. The system's own pairing action is exempt from this check (it is
  system-initiated, not player-initiated). Reflected in FR-010 and FR-011.
- Q: Should the player receive any intermediate status updates while waiting in the queue? → A: No.
  No intermediate updates are sent. The client shows a waiting state until the match notification
  arrives (the pairing event). Reflected in FR-012.

### Session 2026-05-20

- Q: What does the cancel-during-pairing race condition resolve to — conflict with FR-002? → A:
  No conflict. Pairing atomically removes the player's ticket first (FR-011); cancel finds no
  ticket and is a no-op. FR-002 is satisfied because removal already happened. Reflected in
  FR-011 and Edge Cases §Match already formed.
- Q: How precisely does FR-011 bypass the availability restriction? → A: By removing both players'
  tickets from the queue before creating the match. At the moment of match creation neither player
  is "in queue". No special bypass flag needed. Reflected in FR-011.
- Q: What does FR-004 "immediately" mean relative to SC-001's 2-second bound? → A: They refer to
  the same observable event; "immediately" is bounded by SC-001 (≤2 seconds). Reflected in FR-004.
- Q: What specific signal does FR-007 "real-time connection closed" refer to? → A: WebSocket STOMP
  session disconnect event only. Session expiry and HTTP disconnects are out of scope for v1.
  Reflected in FR-007.
- Q: What does the third waiting player (C) receive when A and B are paired simultaneously? → A:
  Nothing — C remains in queue silently until a future opponent arrives. Reflected in Edge Cases
  §Three players arrive simultaneously.
- Q: Is QuickMatchQueue active (detects and triggers) or passive (data only)? → A: Passive data
  structure; the application layer drives detection and triggering. Reflected in Key Entities.
- Q: Does "reuse existing auto-start behaviour" mean reusing the code path? → A: No — it means
  reusing the outcome (both players assigned, match starts without ready-up). A dedicated factory
  method is used. Reflected in Assumptions.
- Q: Should there be a success criterion for FR-010 (queue blocks other match actions)? → A: Yes.
  Added SC-006. Reflected in Success Criteria.
