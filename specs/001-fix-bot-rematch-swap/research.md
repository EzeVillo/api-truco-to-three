# Research: Fix Bot Rematch Auto-Accept and Position Swap

## Root Cause Analysis

### Bug 1 — Bot does not auto-accept when it is player one

**Where the bug lives**: `RematchSession` (domain aggregate) and
`MatchFinishedRematchSessionCreator` (application event handler).

The `RematchSession` aggregate tracks bot participation with a single boolean field
`playerTwoIsBot`. Auto-acceptance logic on `open()` only fires for player two:

```java
// RematchSession.open()
final var p2Choice =
    playerTwoIsBot ? RematchPlayerChoice.WANTS_REMATCH : RematchPlayerChoice.UNDECIDED;
```

`chooseRematch()` has a bot-guard that only applies to player two:

```java
if (!isPlayerOne(actor) && playerTwoIsBot) { return; }
```

`MatchFinishedRematchSessionCreator` only queries the registry for player two:

```java
final var playerTwoIsBot = botRegistry.isBot(playerTwoId);
RematchSession.open(..., playerTwoIsBot, ...);
```

After the first rematch (position swap), the bot is at seat PLAYER_ONE. `playerTwoIsBot = false`,
the bot's choice starts UNDECIDED, no trigger exists to set it to WANTS_REMATCH, and the session
never confirms.

### Bug 2 — Swap appears not to take effect (user observation)

The swap logic in `chooseRematch()` is correct:

```java
new RematchSessionConfirmedEvent(..., playerTwoId, playerOneId, ...)
// → newPlayerOneId = old playerTwo (the bot)
// → newPlayerTwoId = old playerOne (the human)
```

`RematchSessionConfirmedMatchCreator` correctly creates the new match with swapped seats. The bug
the user observes is a **consequence of Bug 1**: after the first rematch game (bot=p1, human=p2 in
the actual game), the second RematchSession opens with bot=p1 and `playerOneIsBot = false` (
unrecognized), so the session never confirms. The user never sees the second rematch start, making
it appear the swap did not persist or was not relevant.

Once Bug 1 is fixed the alternation will work automatically because the swap logic is already
correct.

---

## Design Decision: How to Represent Bot Seat

**Decision**: Add a parallel `playerOneIsBot` boolean field alongside the existing `playerTwoIsBot`.

**Rationale**:

- Minimal blast radius — the existing `playerTwoIsBot` field, its DB column, its tests, and all
  callers remain untouched for the player-two-is-bot path.
- The DB migration is a single
  `ALTER TABLE … ADD COLUMN player_one_is_bot BOOLEAN NOT NULL DEFAULT FALSE`, which is
  non-destructive (existing rows default to `false`).
- The domain model stays symmetric and readable: both flags exist, only one is ever `true` at a
  time (invariant: a match has at most one bot participant).
- Avoids the complexity of replacing the field with a `botSeat` enum or `botPlayerId` nullable
  reference, which would require touching more code (mapper, serialization, notifications).

**Alternatives considered**:

- `PlayerId botPlayerId` (nullable): cleaner conceptually but requires changing the JPA column
  type (UUID vs BOOLEAN) and updating the existing mapper + all tests that pass
  `playerTwoIsBot = true/false`.
- `BotSeat` enum (NONE / PLAYER_ONE / PLAYER_TWO): similarly expressive but replaces two booleans
  with one enum, requiring a DB migration that drops the old column and adds a new one — higher
  migration risk.

---

## Affected Files

| Layer          | File                                      | Change                                                                 |
|----------------|-------------------------------------------|------------------------------------------------------------------------|
| Domain         | `RematchSession.java`                     | Add `playerOneIsBot` field; fix `open()`, `chooseRematch()`, `leave()` |
| Domain         | `RematchSessionOpenedEvent.java`          | Add `playerOneIsBot` to event payload                                  |
| Application    | `MatchFinishedRematchSessionCreator.java` | Query `botRegistry.isBot(playerOneId)` and pass both flags             |
| Infrastructure | `RematchSessionJpaEntity.java`            | Add `playerOneIsBot` column field                                      |
| Infrastructure | `RematchSessionMapper.java`               | Map the new field in both directions                                   |
| Infrastructure | `V17__add_player_one_is_bot.sql`          | `ALTER TABLE` migration                                                |
| Tests          | `RematchSessionTest.java`                 | New tests for bot-as-player-one scenarios                              |
| Tests          | `ChooseRematchCommandHandlerTest.java`    | Add scenario: bot is player one, human confirms                        |

No REST API contract changes. No WebSocket message format changes. No new exceptions needed.

---

## Invariants to Preserve

1. A session can have at most one bot participant; `playerOneIsBot && playerTwoIsBot` is always
   `false`.
2. A bot participant can never leave the session (`BotCannotLeaveRematchSessionException`).
3. The `chooseRematch()` guard must prevent the application layer from ever calling the method with
   the bot's player ID.
4. The position swap in `RematchSessionConfirmedEvent` must continue to invert seats regardless of
   which seat the bot occupies.
