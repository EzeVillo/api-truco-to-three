# Data Model: Fix Bot Rematch Auto-Accept and Position Swap

## Modified Entity: RematchSession

This is a bug fix — no new entities are introduced. The `RematchSession` aggregate gains one
additional boolean field.

### Field Addition

| Field            | Type      | Nullable | Default | Description                                                  |
|------------------|-----------|----------|---------|--------------------------------------------------------------|
| `playerOneIsBot` | `boolean` | No       | `false` | True when the player at seat PLAYER_ONE is a bot participant |

**Invariant**: `playerOneIsBot && playerTwoIsBot` is always `false` — a match has at most one bot.

### Updated `open()` Behavior

When `playerOneIsBot = true`:

- `playerOneChoice` is initialized to `WANTS_REMATCH` (auto-accept)
- `playerTwoChoice` remains `UNDECIDED`

When `playerTwoIsBot = true` (existing behavior, unchanged):

- `playerTwoChoice` is initialized to `WANTS_REMATCH`
- `playerOneChoice` remains `UNDECIDED`

### Updated `chooseRematch()` Behavior

Guard added for bot-as-player-one (mirrors the existing player-two guard):

```
if isPlayerOne(actor) AND playerOneIsBot → return early (no-op)
```

Confirmation check remains unchanged — fires when both choices equal `WANTS_REMATCH`.

### Updated `leave()` Behavior

Guard extended: if `isPlayerOne(actor) && playerOneIsBot` → throw
`BotCannotLeaveRematchSessionException`.

---

## Database Migration

**Migration file**: `V17__add_player_one_is_bot.sql`

```sql
ALTER TABLE rematch_sessions
    ADD COLUMN player_one_is_bot BOOLEAN NOT NULL DEFAULT FALSE;
```

- Non-destructive: all existing rows default to `false` (correct — all historical sessions had the
  bot at player-two seat or no bot at all).
- No index required: the field is not queried independently; it is only read when reconstructing the
  aggregate.

---

## State Transitions

The existing state machine (`OPEN → CONFIRMED | CLOSED_BY_LEAVE | EXPIRED`) is unchanged. The new
field only affects *which player's choice auto-fills on open* and *which guards apply
in `chooseRematch()` and `leave()`*.

```
OPEN (bot=p1, human=p2)
  playerOneChoice = WANTS_REMATCH  ← auto-set on open
  playerTwoChoice = UNDECIDED

  human calls chooseRematch():
    playerTwoChoice = WANTS_REMATCH
    both choices = WANTS_REMATCH → CONFIRMED
    emit RematchSessionConfirmedEvent(newP1=human, newP2=bot)  ← swap

OPEN (human=p1, bot=p2)  [existing behavior]
  playerTwoChoice = WANTS_REMATCH  ← auto-set on open
  playerOneChoice = UNDECIDED

  human calls chooseRematch():
    playerOneChoice = WANTS_REMATCH
    both choices = WANTS_REMATCH → CONFIRMED
    emit RematchSessionConfirmedEvent(newP1=bot, newP2=human)  ← swap
```

---

## Event Payload Change: RematchSessionOpenedEvent

Add `playerOneIsBot` to the opened event so notification subscribers can reflect the correct bot
seat in WebSocket messages.

| Field            | Type      | Description                  |
|------------------|-----------|------------------------------|
| `playerOneIsBot` | `boolean` | Mirrors the new domain field |

Existing `playerTwoIsBot` field on the event is unchanged.
