CREATE TABLE rematch_sessions
(
    id                UUID PRIMARY KEY,
    origin_match_id   UUID        NOT NULL UNIQUE,
    player_one_id     UUID        NOT NULL,
    player_two_id     UUID        NOT NULL,
    player_one_choice VARCHAR(20) NOT NULL,
    player_two_choice VARCHAR(20) NOT NULL,
    player_two_is_bot BOOLEAN     NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL,
    games_to_win      INT         NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    result_match_id   UUID,
    version           INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_rematch_status_expires ON rematch_sessions (status, expires_at);
CREATE INDEX idx_rematch_player_one_open ON rematch_sessions (player_one_id) WHERE status = 'OPEN';
CREATE INDEX idx_rematch_player_two_open ON rematch_sessions (player_two_id) WHERE status = 'OPEN';
