# Feature Specification: Fix Bot Rematch Auto-Accept and Position Swap

**Feature Branch**: `001-fix-bot-rematch-swap`

**Created**: 2026-05-19

**Status**: Draft

**Input**: User description: "BUG: cuando el jugador juega contra un bot, y hace un rematch, y al
bot le toca ser el jugador uno, no acepta de manera automatica, ademas veo que en la primera
revancha el bot es el jugador 2 nuevamente, lo cual entiendo que la idea es que se intercambie quien
es el 1 y quien es el 2 por cada juego"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bot Auto-Accepts Rematch Regardless of Position (Priority: P1)

When a human player requests a rematch against a bot, the bot must immediately and automatically
accept, regardless of whether the bot occupies the player-one or player-two seat in that rematch
session. Currently, the bot only auto-accepts when it is player two; when it is player one (which
happens after the first rematch due to the position swap), the session never confirms and the second
rematch never starts.

**Why this priority**: This is a blocking bug — after the first rematch, no further rematches
against bots are possible. The feature is entirely broken for any session beyond the first rematch.

**Independent Test**: Start a match against a bot, finish it, request a rematch, finish the rematch,
then request a second rematch. The second rematch must start automatically without the human having
to wait or take any extra action.

**Acceptance Scenarios**:

1. **Given** a finished match where the human was player one and the bot was player two, **When**
   the human requests a rematch, **Then** the rematch session is immediately confirmed (bot
   auto-accepts) and a new game begins.

2. **Given** a rematch game where the bot is player one and the human is player two (position
   already swapped), **When** that rematch game finishes and the human requests another rematch, *
   *Then** the bot again auto-accepts immediately and a new game begins.

3. **Given** a rematch session where the bot is player one, **When** the human accepts the rematch,
   **Then** the session confirms without any manual action from the bot side.

---

### User Story 2 - Player Positions Alternate on Each Rematch (Priority: P2)

Each time a rematch is confirmed, the player who was player one becomes player two and vice versa in
the new game. This ensures fair play by alternating who goes first across consecutive games between
the same human and bot.

**Why this priority**: The swap logic already exists in the codebase but its effect may not be
visible or verifiable to the user if Bug 1 (P1) prevents rematches from starting at all. Fixing the
swap independently adds correctness and fairness.

**Independent Test**: Play three consecutive games (original + two rematches). Verify that the
player-one seat alternates: human→bot→human (or bot→human→bot depending on who started first).

**Acceptance Scenarios**:

1. **Given** game 1 where human is player one and bot is player two, **When** a rematch is
   confirmed, **Then** game 2 starts with bot as player one and human as player two.

2. **Given** game 2 where bot is player one and human is player two, **When** a second rematch is
   confirmed, **Then** game 3 starts with human as player one and bot as player two.

3. **Given** any confirmed rematch, **When** the new game starts, **Then** the player who had the
   first turn (mano) in game N is in the non-mano seat in game N+1.

---

### Edge Cases

- What happens when the human leaves the rematch session before it is confirmed (bot is player one)?
  The session should close cleanly, same as when bot is player two.
- What happens if the rematch session expires before the human responds (bot is player one)? The
  session should expire normally, same as when bot is player two.
- What happens on a third, fourth, or Nth rematch? Position swap must continue alternating
  correctly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When a rematch session is opened and one participant is a bot, the system MUST
  immediately register the bot's acceptance regardless of whether the bot is player one or player
  two in that session.

- **FR-002**: When the human participant accepts a rematch in a session where the bot has already
  auto-accepted (and the bot is player one), the system MUST immediately confirm the session and
  create the new game.

- **FR-003**: The system MUST track which participant is a bot independently of their seat
  position (player one or player two) in the rematch session.

- **FR-004**: Each confirmed rematch MUST create a new game with player positions swapped relative
  to the immediately preceding game (player one becomes player two and vice versa).

- **FR-005**: The position swap MUST continue to alternate correctly across any number of
  consecutive rematches between the same human and bot.

- **FR-006**: All existing rematch behaviors (session expiration, human-initiated leave,
  notification events) MUST continue to work correctly when the bot is player one, matching the
  behavior already present for when the bot is player two.

### Key Entities

- **RematchSession**: Tracks two participants, their choices (UNDECIDED / WANTS_REMATCH / LEFT),
  session status, and which participant is a bot. Currently only models "bot is player two"; needs
  to generalize to support bot in either seat.

- **BotRegistry**: Used to determine if a given player ID corresponds to a bot. Already available;
  must be consulted for both player positions when creating a rematch session.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A human player can complete at least three consecutive games against a bot (two
  rematches) without any manual intervention beyond their own rematch request — the bot always
  responds instantly.

- **SC-002**: After any confirmed rematch, the player occupying the player-one seat in the new game
  is always the player who was player two in the preceding game (100% alternation, verifiable
  through match records).

- **SC-003**: No existing test suite failure is introduced — all previously passing tests for
  rematch session creation, confirmation, expiration, and leave continue to pass.

- **SC-004**: The fix introduces zero new exception paths for the standard rematch flow when a bot
  is involved, regardless of which seat the bot occupies.

## Assumptions

- A match between a human and a bot always has exactly one bot participant; there are no bot-vs-bot
  matches.
- The bot can only occupy the player-two seat in an original (non-rematch) game, since that is how
  `CreateBotMatchCommandHandler` creates matches. The player-one bot case only arises after the
  first rematch swap.
- The `BotRegistry` is already available in the rematch session creation path (
  `MatchFinishedRematchSessionCreator`) and can be queried for both player IDs without additional
  wiring.
- The position swap logic already correctly passes swapped IDs to the new match; the bug is only in
  the rematch session's auto-acceptance mechanism, not in which IDs are assigned to which seat in
  the resulting game.
