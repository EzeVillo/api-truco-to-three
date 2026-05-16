CREATE TABLE player_stats
(
    player_id      UUID PRIMARY KEY,
    matches_played INT NOT NULL DEFAULT 0,
    matches_won    INT NOT NULL DEFAULT 0,
    matches_lost   INT NOT NULL DEFAULT 0,
    version        INT NOT NULL DEFAULT 0
);

CREATE TABLE processed_match_stats
(
    player_id    UUID      NOT NULL,
    match_id     UUID      NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (player_id, match_id)
);
