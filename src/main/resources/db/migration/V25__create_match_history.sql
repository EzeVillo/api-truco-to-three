CREATE TABLE player_match_history
(
    player_id UUID PRIMARY KEY,
    state     JSONB NOT NULL,
    version   INT   NOT NULL DEFAULT 0
);
