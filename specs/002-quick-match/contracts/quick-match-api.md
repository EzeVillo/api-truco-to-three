# API Contract: Quick Match

**Branch**: `002-quick-match` | **Date**: 2026-05-19

---

## REST Endpoints

All endpoints require `Authorization: Bearer <jwt>`.

---

### POST /api/matches/quick — Enter Quick Match queue

Enqueues the authenticated player for the given `gamesToPlay`. If a compatible opponent is already
waiting, a match is created immediately and returned.

**Request**

```
POST /api/matches/quick
Content-Type: application/json
Authorization: Bearer <token>

{
  "gamesToPlay": 3          // integer, required — games needed to win the match
}
```

**Response 200 — Searching** (player added to queue, no opponent yet)

```json
{
  "status": "SEARCHING",
  "matchId": null,
  "enqueuedAt": "2026-05-19T12:00:00Z"
}
```

**Response 200 — Matched immediately** (opponent already in queue, match created)

```json
{
  "status": "MATCHED",
  "matchId": "550e8400-e29b-41d4-a716-446655440000",
  "enqueuedAt": "2026-05-19T12:00:00Z"
}
```

**Response 200 — Already in queue (idempotent)** (player already searching; FR-008)

```json
{
  "status": "SEARCHING",
  "matchId": null,
  "enqueuedAt": "2026-05-19T11:58:30Z"
  // original enqueue time
}
```

**Response 409 Conflict** — player has an active match, open rematch session, active tournament,
or any other `PlayerAvailabilityChecker` violation

```json
{
  "error": "PLAYER_NOT_AVAILABLE",
  "reason": "ACTIVE_MATCH"
  // ACTIVE_MATCH | IN_QUEUE | IN_TOURNAMENT | ...
}
```

**Response 400 Bad Request** — invalid `gamesToPlay` value

```json
{
  "error": "INVALID_GAMES_TO_PLAY"
}
```

---

### DELETE /api/matches/quick — Cancel Quick Match search

Removes the authenticated player from the Quick Match queue. Idempotent: safe to call even if the
player is not currently in the queue.

**Request**

```
DELETE /api/matches/quick
Authorization: Bearer <token>
```

**Response 204 No Content** — player removed from queue (or was not in queue; no-op)

**Notes**: If the player has already been matched (race condition: cancel arrives after pairing),
the match is NOT cancelled. The response is still 204 — the client must handle the pairing
notification separately via WebSocket.

---

## WebSocket Notification — Match Found

When two players are paired via Quick Match, both receive a notification on their personal match
channel. This reuses the **existing** WebSocket notification infrastructure.

**Destination** (personal, per-user): `/user/queue/match`

**Payload** — same `MatchWsEvent` format used for all match events:

```json
{
  "type": "MATCH_STARTED",
  "matchId": "550e8400-e29b-41d4-a716-446655440000",
  "playerOne": "alice",
  "playerTwo": "bob",
  "gamesToPlay": 3,
  "status": "IN_PROGRESS"
}
```

Both players (the one who was in the queue and the one who triggered the match by joining the
queue) receive this notification simultaneously when the match is created.

**No new WebSocket channel is introduced.** Clients already subscribed to `/user/queue/match`
receive the pairing notification automatically.

---

## Error Codes Reference

| Code                    | HTTP Status | Meaning                                                   |
|-------------------------|-------------|-----------------------------------------------------------|
| `PLAYER_NOT_AVAILABLE`  | 409         | Player cannot join queue (active match, tournament, etc.) |
| `INVALID_GAMES_TO_PLAY` | 400         | gamesToPlay value is not valid                            |

---

## Notes

- `gamesToPlay` must be a value accepted by the existing `GamesToPlay` value object (same values
  accepted for private/public match creation).
- The Quick Match endpoint is NOT idempotent for *pairing* — if the client sends two
  near-simultaneous
  requests, both pass through, but the second one hits the idempotency check and returns SEARCHING.
- Matches created via Quick Match do not appear in `GET /api/matches/public`.
