# Tasks: Fix Bot Rematch Auto-Accept and Position Swap

**Input**: Design documents from `specs/001-fix-bot-rematch-swap/`

**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓

**Organization**: Tasks grouped by user story — each story is independently completable and
testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: DB migration and the updated domain event are shared prerequisites — no US1 or US2 task
can be completed without them.

**⚠️ CRITICAL**: Both tasks must be complete before user story work begins.

- [x] T001 Create `src/main/resources/db/migration/V17__add_player_one_is_bot.sql` —
  `ALTER TABLE rematch_sessions ADD COLUMN player_one_is_bot BOOLEAN NOT NULL DEFAULT FALSE;`
- [x] T002 Add `playerOneIsBot` boolean field and getter to `RematchSessionOpenedEvent` in
  `src/main/java/com/villo/truco/domain/model/rematch/events/RematchSessionOpenedEvent.java`; update
  its constructor to accept the new parameter alongside the existing `playerTwoIsBot`

**Checkpoint**: Migration file exists; `RematchSessionOpenedEvent` compiles with the new field. No
other changes yet.

---

## Phase 2: User Story 1 — Bot Auto-Accepts Regardless of Position (Priority: P1) 🎯 MVP

**Goal**: The bot immediately accepts a rematch regardless of whether it is in the player-one or
player-two seat. After this phase the second rematch (and all subsequent ones) against a bot must
start automatically.

**Independent Test**: Start a bot match → finish it → request rematch (first rematch) → finish it →
request rematch again (second rematch). The second rematch must start without any extra user action.

### Implementation for User Story 1

- [x] T003 [US1] Add `playerOneIsBot` field to `RematchSession` aggregate in
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java`: add the private field,
  update the private constructor and `reconstruct()` factory to accept it (add before
  `playerTwoIsBot` parameter for symmetry)
- [x] T004 [US1] Update `RematchSession.open()` in
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java`: add `playerOneIsBot`
  parameter; set `playerOneChoice = playerOneIsBot ? WANTS_REMATCH : UNDECIDED`; pass
  `playerOneIsBot` to the emitted `RematchSessionOpenedEvent` (depends on T002, T003)
- [x] T005 [US1] Update `RematchSession.chooseRematch()` in
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java`: add guard
  `if (isPlayerOne(actor) && playerOneIsBot) { return; }` immediately before the existing player-two
  guard (depends on T003)
- [x] T006 [US1] Update `RematchSession.leave()` in
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java`: add guard
  `if (playerOneIsBot && isPlayerOne(actor)) { throw new BotCannotLeaveRematchSessionException(); }`
  alongside the existing player-two guard (depends on T003)
- [x] T007 [US1] Add `isPlayerOneIsBot()` getter to `RematchSession` in
  `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java` (depends on T003)
- [x] T008 [US1] Update `MatchFinishedRematchSessionCreator.handle()` in
  `src/main/java/com/villo/truco/application/eventhandlers/MatchFinishedRematchSessionCreator.java`:
  add `final var playerOneIsBot = botRegistry.isBot(playerOneId);` and pass it to
  `RematchSession.open()` as the new parameter (depends on T004)
- [x] T009 [P] [US1] Add `player_one_is_bot` field and getter/setter to `RematchSessionJpaEntity` in
  `src/main/java/com/villo/truco/infrastructure/persistence/entities/RematchSessionJpaEntity.java`:
  `@Column(name = "player_one_is_bot", nullable = false) private boolean playerOneIsBot;` (depends
  on T001)
- [x] T010 [US1] Update `RematchSessionMapper.toEntity()` and `toDomain()` in
  `src/main/java/com/villo/truco/infrastructure/persistence/mappers/RematchSessionMapper.java`: map
  `playerOneIsBot` in both directions; pass it to `RematchSession.reconstruct()` (depends on T007,
  T009)
- [x] T011 [US1] Update `RematchNotificationEventTranslator.handleOpened()` in
  `src/main/java/com/villo/truco/application/eventhandlers/RematchNotificationEventTranslator.java`:
  change recipients logic to exclude player one when `event.isPlayerOneIsBot()`, mirroring the
  existing exclusion for player two (depends on T002)

### Tests for User Story 1

- [x] T012 [P] [US1] Add test `sets playerOne choice to WANTS_REMATCH when playerOne is bot` to the
  `Open` nested class in
  `src/test/java/com/villo/truco/domain/model/rematch/RematchSessionTest.java`: open session with
  `playerOneIsBot=true`, assert `playerOneChoice == WANTS_REMATCH` and
  `playerTwoChoice == UNDECIDED` (depends on T004)
- [x] T013 [P] [US1] Add test
  `bot match with bot as player one confirms immediately when human (player two) chooses` to
  `ChooseRematch` nested class in
  `src/test/java/com/villo/truco/domain/model/rematch/RematchSessionTest.java`: open with
  `playerOneIsBot=true`, call `chooseRematch(playerTwo, ...)`, assert status == CONFIRMED (depends
  on T004, T005)
- [x] T014 [P] [US1] Add test `calling chooseRematch with bot player-one actor is a no-op` to
  `ChooseRematch` nested class in
  `src/test/java/com/villo/truco/domain/model/rematch/RematchSessionTest.java`: open with
  `playerOneIsBot=true`, clear events, call `chooseRematch(playerOne, ...)`, assert no domain events
  emitted and status == OPEN (depends on T005)
- [x] T015 [P] [US1] Add test
  `throws BotCannotLeaveRematchSessionException when bot as player one tries to leave` to `Leave`
  nested class in `src/test/java/com/villo/truco/domain/model/rematch/RematchSessionTest.java` (
  depends on T006)
- [x] T016 [US1] Add test
  `confirms session immediately when bot is player one and human (player two) chooses` to
  `src/test/java/com/villo/truco/application/usecases/commands/ChooseRematchCommandHandlerTest.java`:
  set up session with `playerOneIsBot=true`, invoke handler with playerTwo actor, assert session
  status CONFIRMED and events published (depends on T004, T005, T008, T010)

**Checkpoint**: `./gradlew test --tests "com.villo.truco.domain.model.rematch.RematchSessionTest"`
and
`./gradlew test --tests "com.villo.truco.application.usecases.commands.ChooseRematchCommandHandlerTest"`
both pass. A second rematch against a bot now auto-starts.

---

## Phase 3: User Story 2 — Player Positions Alternate on Each Rematch (Priority: P2)

**Goal**: Verify that the position swap (already coded in `chooseRematch`) works correctly across
multiple rematches now that US1 unblocks it. Ensure the notification event carries correct swap
information regardless of which seat the bot occupies.

**Independent Test**: After completing US1, run three consecutive games (original + two rematches).
Player-one seat must alternate: human→bot→human (or bot→human→bot).

### Implementation for User Story 2

- [x] T017 [US2] Verify `RematchSessionConfirmedEvent` emission in `RematchSession.chooseRematch()`
  at `src/main/java/com/villo/truco/domain/model/rematch/RematchSession.java`: confirm the swap is
  emitted correctly when bot is player one — `newPlayerOneId = playerTwoId (human)`,
  `newPlayerTwoId = playerOneId (bot)` — no code change expected; this is a correctness check (
  depends on T005)

### Tests for User Story 2

- [x] T018 [US2] Add test
  `both players choosing confirms with inverted seats when bot is player one` to `ChooseRematch`
  nested class in `src/test/java/com/villo/truco/domain/model/rematch/RematchSessionTest.java`: open
  with `playerOneIsBot=true`, call `chooseRematch(playerTwo)`, assert
  `confirmed.getNewPlayerOneId() == playerTwo` and `confirmed.getNewPlayerTwoId() == playerOne` —
  bot swaps back to player-two seat in the next game (depends on T004, T005)
- [x] T019 [US2] Add test `handleOpened sends notification only to human when bot is player one` to
  `src/test/java/com/villo/truco/application/eventhandlers/RematchNotificationEventTranslatorTest.java` (
  create file if not present): fire a `RematchSessionOpenedEvent` with `playerOneIsBot=true`, assert
  the published `MatchEventNotification` recipient list contains only `playerTwoId` (human), not
  `playerOneId` (bot) (depends on T011)

**Checkpoint**: `./gradlew test` passes in full. Position alternation is end-to-end correct.

---

## Phase 4: Polish & Verification

**Purpose**: Full suite pass and coverage gate.

- [x] T020 Run `./gradlew test` to confirm all tests pass and JaCoCo 70% coverage gate is met; fix
  any coverage gaps if needed
- [x] T021 [P] Verify ArchUnit `CleanArchitectureTest` passes — confirm no Spring imports leaked
  into domain classes and no cross-aggregate imports introduced

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — start immediately
- **User Story 1 (Phase 2)**: Depends on T001 (migration) and T002 (event update) — all Phase 1 must
  complete first
- **User Story 2 (Phase 3)**: Depends on all Phase 2 tasks (US1 fix unblocks the swap verification)
- **Polish (Phase 4)**: Depends on all Phase 3 tasks

### Within User Story 1

- T003 (field) → T004, T005, T006, T007 (all depend on field)
- T004 (open) → T008 (creator), T012, T013, T014 (tests)
- T005 (choose guard) → T013, T014, T016 (tests)
- T006 (leave guard) → T015 (test)
- T007 (getter) → T010 (mapper)
- T009 (JPA) → T010 (mapper)
- T010 (mapper) → T016 (integration test)
- T011 (notification) → T019 (notification test)

### Parallel Opportunities

Within Phase 2 (US1), once T003 is done, T004–T007 can proceed in parallel. T009 (JPA) can be done
in parallel with T003–T007 as it only depends on T001. Tests T012–T015 can be written in parallel
with each other once their respective domain methods are implemented.

---

## Parallel Example: User Story 1

```
After T001, T002 complete:
  Parallel group A (domain logic, depends on T003):
    T004 — fix open()
    T005 — fix chooseRematch()
    T006 — fix leave()
    T007 — add getter

  Parallel group B (infrastructure, depends on T001):
    T009 — JPA entity field

After group A and group B complete:
  T008 — fix MatchFinishedRematchSessionCreator (depends on T004)
  T010 — fix mapper (depends on T007, T009)
  T011 — fix notification translator (depends on T002)

After T004, T005, T006 complete:
  Parallel tests:
    T012 — open() with bot-as-p1
    T013 — confirm immediately when human chooses
    T014 — chooseRematch with bot actor is no-op
    T015 — leave throws for bot-as-p1

After T008, T010 complete:
    T016 — handler integration test
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001, T002)
2. Complete Phase 2: User Story 1 (T003–T016)
3. **STOP and VALIDATE**: Run `./gradlew test`; manually verify second rematch against bot starts
   automatically
4. Proceed to Phase 3 (US2) once US1 is confirmed

### Incremental Delivery

1. T001 + T002 → Foundation ready (no functional change yet)
2. T003–T011 → Core fix live (bot auto-accepts in both seats)
3. T012–T016 → Tests green for US1
4. T017–T019 → Swap correctness verified for US2
5. T020–T021 → Full suite + coverage gate passes

---

## Notes

- [P] tasks operate on different files and have no unresolved dependencies — safe to run in parallel
- `playerOneIsBot` is added as a parallel to `playerTwoIsBot` — **do not rename or remove** the
  existing field to avoid touching unrelated tests
- The position swap logic in `chooseRematch()` (confirmed event emits `playerTwoId` as
  `newPlayerOneId`) is **already correct** — US2 only adds verification tests, no implementation
  change expected
- Default `FALSE` for the migration column is correct for all existing rows
- Run `./gradlew test` after each phase checkpoint, not just at the end
