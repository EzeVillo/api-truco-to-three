# Tasks: Quick Match (Partida Rápida)

**Input**: Design documents from `specs/002-quick-match/`

**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Base package**: `com.villo.truco`  
**Source root**: `src/main/java/com/villo/truco/`  
**Test root**: `src/test/java/com/villo/truco/`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies at that point)
- **[Story]**: Which user story this task belongs to (US1/US2/US3)

---

## Phase 1: Setup

**Purpose**: Verify prerequisites. No new Gradle dependencies or config files are needed — the
feature uses only JDK built-ins and existing Spring/JPA infrastructure.

- [x] T001 Verify the `rematch` package structure (`domain/model/rematch/`,
  `domain/ports/RematchSessionRepository.java`) as the reference pattern for the new `quickmatch`
  bounded context

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain primitives and in-memory queue that MUST exist before any user story handler
can be written. Also includes the `PlayerAvailabilityChecker` change that enforces FR-010 across
ALL existing match-creation paths.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Create `QuickMatchTicket` in `domain/model/quickmatch/QuickMatchTicket.java` —
  immutable class with fields `PlayerId playerId`, `GamesToPlay gamesToPlay`, `Instant enqueuedAt`,
  `String webSocketSessionId` (nullable); constructor validates non-null playerId and gamesToPlay;
  no Spring imports
- [x] T003 [P] Create `PlayerAlreadyInQueueException` in
  `domain/model/quickmatch/exceptions/PlayerAlreadyInQueueException.java` — follows same base class
  as `PlayerAlreadyInActiveMatchException`; no fields beyond the message
- [x] T004 Create `QuickMatchQueuePort` in `domain/ports/QuickMatchQueuePort.java` — domain output
  port interface with five methods: `enqueue(QuickMatchTicket)`,
  `tryDequeue(PlayerId): Optional<QuickMatchTicket>`,
  `tryMatchOpponent(PlayerId enqueuingPlayer, GamesToPlay): Optional<QuickMatchTicket>`,
  `isPlayerQueued(PlayerId): boolean`,
  `tryDequeueBySessionId(String sessionId): Optional<QuickMatchTicket>`; no Spring imports (depends
  on T002)
- [x] T005 Create `InMemoryQuickMatchQueueAdapter` in
  `infrastructure/persistence/repositories/InMemoryQuickMatchQueueAdapter.java` — `@Component`
  implementing `QuickMatchQueuePort`; internal structure:
  `ConcurrentHashMap<GamesToPlay, ConcurrentLinkedDeque<QuickMatchTicket>> queues` (per-gamesToPlay
  FIFO queue), `ConcurrentHashMap<PlayerId, QuickMatchTicket> byPlayer` (O(1) lookup),
  `ConcurrentHashMap<String, PlayerId> bySessionId` (disconnect lookup); `tryMatchOpponent()`
  synchronizes on the per-gamesToPlay deque to make find-and-remove atomic (depends on T004)
- [x] T006 [P] Create `QuickMatchSearchDTO` in `application/dto/QuickMatchSearchDTO.java` and
  `QuickMatchStatus` enum in `application/dto/QuickMatchStatus.java` (`SEARCHING`, `MATCHED`) — used
  as the return type of both use cases; no Spring imports
- [x] T007 Modify `application/usecases/commands/PlayerAvailabilityChecker.java` — add
  `QuickMatchQueuePort quickMatchQueuePort` constructor parameter; add check
  `if (this.quickMatchQueuePort.isPlayerQueued(playerId)) throw new PlayerAlreadyInQueueException()`
  in `ensureAvailable()` immediately after the `rematchSessionRepository.findOpenByPlayer()` check (
  line 47); no other methods change (depends on T003, T004)
- [x] T008 Modify `infrastructure/config/PlayerAvailabilityConfiguration.java` — add
  `QuickMatchQueuePort quickMatchQueuePort` parameter to the `@Bean` factory method and pass it as
  the last constructor argument to `PlayerAvailabilityChecker`; the bean is already created via
  `@Bean` factory so this is a one-line change (depends on T005, T007)

**Checkpoint**: Run `./gradlew test --tests "PlayerAvailabilityCheckerTest"` — existing tests should
still pass.

---

## Phase 3: User Story 1 — Find and Start a Quick Match (Priority: P1) 🎯 MVP

**Goal**: Player clicks Quick Match, selects gamesToPlay, is either immediately paired with a
waiting opponent (match starts, both get notified) or enters the queue (waits for opponent).

**Independent Test**: Two players POST `/api/matches/quick` with the same `gamesToPlay` — both
receive a 200 response with `status=MATCHED` and the same `matchId`. A third POST from player A
while already searching returns `status=SEARCHING` with the original `enqueuedAt`.

### Implementation for User Story 1

- [x] T009 [P] Add
  `Match.quickMatch(PlayerId playerOne, PlayerId playerTwo, GamesToPlay gamesToPlay)` static factory
  to `domain/model/match/Match.java` — creates a match with both players assigned,
  `Visibility.PRIVATE`, transitions directly to `IN_PROGRESS` by calling `startInternal()`; must NOT
  publish `PublicMatchLobbyOpenedEvent`; MUST publish `MatchStartedEvent` (or the same event
  `joinPublic()` produces) so the existing `MatchNotificationEventTranslator` +
  `StompMatchNotificationHandler` delivers the match to both players on `/user/queue/match`; study
  `Match.joinPublic()` (which auto-starts) as the reference
- [x] T010 [P] Create `EnqueueForQuickMatchCommand` in
  `application/commands/EnqueueForQuickMatchCommand.java` — record with fields `PlayerId playerId`,
  `GamesToPlay gamesToPlay`, `String webSocketSessionId` (nullable); provide a constructor that also
  accepts raw `String gamesToPlay` for HTTP binding (same pattern as `CreateMatchCommand`)
- [x] T011 [P] Create `EnqueueForQuickMatchUseCase` in
  `application/ports/in/EnqueueForQuickMatchUseCase.java` — interface extending
  `UseCase<EnqueueForQuickMatchCommand, QuickMatchSearchDTO>`; no implementation here (depends on
  T006, T010)
- [x] T012 Create `EnqueueForQuickMatchCommandHandler` in
  `application/usecases/commands/EnqueueForQuickMatchCommandHandler.java` implementing
  `EnqueueForQuickMatchUseCase`; orchestration: (1) if `queuePort.isPlayerQueued(playerId)` → return
  existing ticket as `SEARCHING` (idempotency FR-008); (2)
  `playerAvailabilityChecker.ensureAvailable(playerId)`; (3)
  `opponent = queuePort.tryMatchOpponent(playerId, gamesToPlay)`; (4a) if opponent present:
  `match = Match.quickMatch(opponent.playerId(), playerId, gamesToPlay)` →
  `matchRepository.save(match)` →
  `matchEventNotifier.publishDomainEvents(match.getDomainEvents())` → return `MATCHED`; (4b) else:
  `queuePort.enqueue(new QuickMatchTicket(playerId, gamesToPlay, Instant.now(), sessionId))` →
  return `SEARCHING`; no Spring annotations (depends on T004, T007, T009, T010, T011)
- [x] T013 Create `QuickMatchController` in `infrastructure/http/QuickMatchController.java` —
  `@RestController` `@RequestMapping("/api/matches/quick")`; POST method extracts `Jwt` principal,
  maps `gamesToPlay` from request body, calls `EnqueueForQuickMatchUseCase`; follow
  `MatchController` exactly for auth extraction and response mapping; only POST endpoint in this
  task (depends on T011, T012 via config)
- [x] T014 Create `QuickMatchUseCaseConfiguration` in
  `infrastructure/config/QuickMatchUseCaseConfiguration.java` — `@Configuration` bean factory;
  creates `EnqueueForQuickMatchCommandHandler` with all its dependencies (`QuickMatchQueuePort`,
  `PlayerAvailabilityChecker`, `MatchRepository`, `MatchEventNotifier`), wraps with
  `retryTransactionalPipeline`, exposes as `EnqueueForQuickMatchUseCase` bean; follow
  `MatchUseCaseConfiguration` as the exact reference (depends on T012)

### Tests for User Story 1

- [x] T015 [P] Create `EnqueueForQuickMatchCommandHandlerTest` in
  `application/usecases/commands/EnqueueForQuickMatchCommandHandlerTest.java` — use Mockito; test
  cases: (a) pairing path — opponent in queue → MATCHED returned, match created, events published; (
  b) no-opponent path — queue empty → SEARCHING returned, ticket enqueued; (c) idempotency — player
  already in queue → SEARCHING returned, no second enqueue; (d) player unavailable —
  `ensureAvailable` throws → exception propagates; mock `QuickMatchQueuePort`,
  `PlayerAvailabilityChecker`, `MatchRepository`, `MatchEventNotifier`
- [x] T016 [P] Create `InMemoryQuickMatchQueueAdapterTest` in
  `infrastructure/persistence/repositories/InMemoryQuickMatchQueueAdapterTest.java` — test cases: (
  a) enqueue + isPlayerQueued returns true; (b) FIFO ordering — two tickets for same gamesToPlay,
  `tryMatchOpponent` returns the earlier one; (c) tryMatchOpponent does not return the
  enqueuingPlayer's own ticket; (d) tryDequeue removes from both maps; (e) tryDequeueBySessionId
  removes by session; (f) concurrent test — two threads call enqueue + tryMatchOpponent
  simultaneously for same gamesToPlay, verify exactly one match occurs
- [x] T017 [P] Add `QuickMatchTicketTest` in `domain/model/quickmatch/QuickMatchTicketTest.java` —
  test: null playerId throws, null gamesToPlay throws, valid construction succeeds, fields
  accessible
- [x] T018 [P] Create `QuickMatchControllerTest` in
  `infrastructure/http/QuickMatchControllerTest.java` — Spring MVC test (`@WebMvcTest`); test POST:
  returns 200 SEARCHING when use case returns SEARCHING; returns 200 MATCHED with matchId when use
  case returns MATCHED; returns 409 when use case throws `PlayerAlreadyInActiveMatchException` or
  `PlayerAlreadyInQueueException`; follow existing controller test pattern in project

**Checkpoint**: Run `./gradlew test --tests "EnqueueForQuickMatchCommandHandlerTest"` and
`./gradlew test --tests "InMemoryQuickMatchQueueAdapterTest"` — all pass. Manual test: POST
`/api/matches/quick` with one user → 200 SEARCHING; POST with second user same gamesToPlay → 200
MATCHED; both users receive match notification on `/user/queue/match`.

---

## Phase 4: User Story 2 — Cancel Quick Match Search (Priority: P2)

**Goal**: Player in queue presses cancel → immediately exits queue, can start other match actions.

**Independent Test**: Player A enters queue, then DELETEs `/api/matches/quick` → 204 returned.
Player B then enters queue with the same `gamesToPlay` → response is SEARCHING (A is not in queue),
not MATCHED.

### Implementation for User Story 2

- [x] T019 [P] Create `CancelQuickMatchSearchCommand` in
  `application/commands/CancelQuickMatchSearchCommand.java` — record with field `PlayerId playerId`
- [x] T020 [P] Create `CancelQuickMatchSearchUseCase` in
  `application/ports/in/CancelQuickMatchSearchUseCase.java` — interface extending
  `UseCase<CancelQuickMatchSearchCommand, Void>` (depends on T019)
- [x] T021 Create `CancelQuickMatchSearchCommandHandler` in
  `application/usecases/commands/CancelQuickMatchSearchCommandHandler.java` implementing
  `CancelQuickMatchSearchUseCase` — single call: `queuePort.tryDequeue(command.playerId())` (no-op
  if not in queue); return null (Void); no Spring annotations (depends on T004, T019, T020)
- [x] T022 Add DELETE endpoint to `infrastructure/http/QuickMatchController.java` — `@DeleteMapping`
  method extracts `Jwt` principal, calls `CancelQuickMatchSearchUseCase`, returns 204 No Content;
  add `CancelQuickMatchSearchUseCase` as second constructor parameter (depends on T020, T021 via
  config)
- [x] T023 Add `CancelQuickMatchSearchCommandHandler` bean to
  `infrastructure/config/QuickMatchUseCaseConfiguration.java` — create handler with
  `QuickMatchQueuePort`, wrap with pipeline, expose as `CancelQuickMatchSearchUseCase`; inject into
  `QuickMatchController` bean (depends on T021)

### Tests for User Story 2

- [x] T024 [P] Create `CancelQuickMatchSearchCommandHandlerTest` in
  `application/usecases/commands/CancelQuickMatchSearchCommandHandlerTest.java` — test cases: (a)
  player in queue → ticket removed (`tryDequeue` called once); (b) player not in queue →
  `tryDequeue` returns empty, no exception thrown (idempotent); mock `QuickMatchQueuePort`
- [x] T025 [P] Add DELETE test cases to `infrastructure/http/QuickMatchControllerTest.java` — DELETE
  returns 204 when cancel succeeds; DELETE returns 204 even when use case is no-op (player not in
  queue)

**Checkpoint**: Run `./gradlew test --tests "CancelQuickMatchSearchCommandHandlerTest"` — all pass.
Manual test: POST to enqueue, DELETE to cancel → 204; second POST with same player → SEARCHING (
queue clean).

---

## Phase 5: User Story 3 — Automatic Queue Cleanup on Disconnection (Priority: P3)

**Goal**: When a queued player's WebSocket connection closes, their ticket is removed automatically
with no ghost entries left in the queue.

**Independent Test**: Player A enters queue. Simulate server-side disconnect of A's WebSocket
session. Verify A is no longer in queue (POST with another player → SEARCHING, not MATCHED with A).
Player A can re-enter queue after reconnecting.

**Dependency**: Requires US2 (`CancelQuickMatchSearchCommand` and its use case) to be complete.

### Implementation for User Story 3

- [x] T026 Create `QuickMatchSessionDisconnectEventListener` in
  `infrastructure/websocket/QuickMatchSessionDisconnectEventListener.java` — `@Component`;
  `@EventListener(SessionDisconnectEvent.class)` method: (1) wrap event with
  `StompHeaderAccessor.wrap(event.getMessage())`; (2) extract `JwtUser` principal via
  `accessor.getUser()`; (3) if principal is null or user not identified, return silently; (4)
  extract `PlayerId` from principal (same pattern as `SpectateSessionTerminationEventListener`); (5)
  call `cancelQuickMatchSearch.handle(new CancelQuickMatchSearchCommand(playerId))`; wrap in
  try-catch so a missing-from-queue scenario never breaks the disconnect lifecycle; use
  `CancelQuickMatchSearchUseCase` as the injected dependency (depends on T020, T021)
- [x] T027 Add `QuickMatchSessionDisconnectEventListener` bean to
  `infrastructure/config/QuickMatchUseCaseConfiguration.java` (or a dedicated
  `QuickMatchWebSocketConfiguration.java` if preferred) — inject `CancelQuickMatchSearchUseCase`
  bean (depends on T026)

### Tests for User Story 3

- [x] T028 [P] Create `QuickMatchSessionDisconnectEventListenerTest` in
  `infrastructure/websocket/QuickMatchSessionDisconnectEventListenerTest.java` — test cases: (a)
  disconnect event with valid authenticated principal → `cancelQuickMatchSearch.handle()` called
  with correct playerId; (b) disconnect event with null principal → no call, no exception; (c)
  `cancelQuickMatchSearch.handle()` throws → exception is swallowed, no propagation; mock
  `CancelQuickMatchSearchUseCase`

**Checkpoint**: Run `./gradlew test --tests "QuickMatchSessionDisconnectEventListenerTest"` — all
pass. Manual test: enter queue, force-close WebSocket tab, check queue state via second user →
SEARCHING confirms cleanup.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Remaining test coverage, documentation, and full build verification.

- [x] T029 [P] Add queue-related test cases to
  `application/usecases/commands/PlayerAvailabilityCheckerTest.java` — add: (a) player with open
  QuickMatch ticket → `ensureAvailable()` throws `PlayerAlreadyInQueueException`; (b) player with no
  open ticket → passes check normally; mock `QuickMatchQueuePort.isPlayerQueued()`
- [x] T030 [P] Add `Match.quickMatch()` test cases to `domain/model/match/MatchTest.java` — test: (
  a) factory creates match with both players; (b) status is `IN_PROGRESS`; (c) visibility is
  `PRIVATE`; (d) `isPublicLobbyOpen()` returns false; (e) correct domain events published
- [x] T031 [P] Update `README.md` — add `quickmatch` to bounded context list; add
  `POST /api/matches/quick` and `DELETE /api/matches/quick` to REST endpoint reference; note
  in-memory queue limitation (lost on restart)
- [x] T032 [P] Update `docs/CONTRATOS_API.md` (if it exists) — add `POST /api/matches/quick`
  request/response shape; add `DELETE /api/matches/quick`; add `MATCH_STARTED` WebSocket event note
  for quick-match pairing on `/user/queue/match`; mark new `PlayerAlreadyInQueueException` error
  code
- [x] T033 Run `./gradlew test --tests "com.villo.truco.CleanArchitectureTest"` — verify no ArchUnit
  violations from new packages; fix any violations before proceeding
- [x] T034 Run `./gradlew build` — verify full build compiles, all tests pass, and JaCoCo 70% line
  coverage gate is met; fix any coverage gaps

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Foundational — core MVP
- **US2 (Phase 4)**: Depends on Foundational — can start in parallel with US1 after Phase 2
- **US3 (Phase 5)**: Depends on US2 (`CancelQuickMatchSearchUseCase` must exist)
- **Polish (Phase 6)**: Depends on all user story phases

### User Story Dependencies

- **US1 (P1)**: After Foundational only
- **US2 (P2)**: After Foundational only (independent of US1)
- **US3 (P3)**: After US2 (reuses cancel use case)

### Key Within-Story Dependencies

```
T002, T003 (parallel) → T004 → T005
T005 + T007 → T008 (PlayerAvailabilityConfiguration)
T009, T010 (parallel) → T011
T011 + T009 + T004 → T012 → T013, T014
T019, T020 (parallel) → T021 → T022, T023
T021 → T026 → T027
```

### Parallel Opportunities

```bash
# Phase 2 — can start these in parallel:
T002 (QuickMatchTicket)
T003 (PlayerAlreadyInQueueException)
T006 (QuickMatchSearchDTO + QuickMatchStatus enum)

# Phase 3 — can start in parallel after T004 is done:
T009 (EnqueueForQuickMatchCommand)
T010 (EnqueueForQuickMatchUseCase)  ← needs T009
# Also parallel with Phase 3 implementation:
T015 (EnqueueForQuickMatchCommandHandlerTest)
T016 (InMemoryQuickMatchQueueAdapterTest)
T017 (QuickMatchTicketTest)
T018 (QuickMatchControllerTest)

# Phase 4 — parallel:
T019 (CancelQuickMatchSearchCommand)
T020 (CancelQuickMatchSearchUseCase)  ← needs T019
T024 (CancelQuickMatchSearchCommandHandlerTest)
T025 (QuickMatchControllerTest DELETE cases)

# Phase 6 — all parallel:
T029, T030, T031, T032
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2 (Foundational) — ~7 tasks
2. Phase 3 (US1: enqueue + match creation) — ~10 tasks
3. **STOP AND VALIDATE**: Two players POST → both receive MATCHED notification on WebSocket
4. Deploy MVP — basic Quick Match is working

### Incremental Delivery

1. Foundation → US1 → **Demo: Quick Match works**
2. Add US2 (cancel) → **Demo: Players can exit queue**
3. Add US3 (disconnect cleanup) → **Demo: Ghost entries eliminated**
4. Polish → Full build clean

---

## Notes

- [P] tasks = different files, no shared dependencies at that point — safe to parallelize
- Each user story phase is independently testable before moving to the next
- `Match.quickMatch()` (T009) can be developed and unit-tested before the handler (T012) exists
- The disconnect listener (T026) depends on US2 being complete — do not start it before T021
- Run `CleanArchitectureTest` (T033) before the final build — easier to fix violations incrementally
- JaCoCo coverage gate may require additional test cases; check the report at
  `build/reports/jacoco/test/html/index.html`
