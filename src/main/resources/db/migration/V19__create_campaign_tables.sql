CREATE TABLE campaign_progress
(
    player_id                 UUID PRIMARY KEY,
    points                    INT     NOT NULL DEFAULT 0,
    active_challenge_match_id UUID,
    active_challenge_rival_id UUID,
    top_one_reached           BOOLEAN NOT NULL DEFAULT FALSE,
    all_rivals_defeated       BOOLEAN NOT NULL DEFAULT FALSE,
    version                   INT     NOT NULL DEFAULT 0
);

CREATE TABLE campaign_rival_records
(
    player_id UUID NOT NULL REFERENCES campaign_progress (player_id) ON DELETE CASCADE,
    rival_id  UUID NOT NULL,
    wins      INT  NOT NULL DEFAULT 0,
    losses    INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (player_id, rival_id)
);

CREATE TABLE campaign_matches
(
    match_id  UUID PRIMARY KEY,
    player_id UUID NOT NULL
);

CREATE INDEX idx_campaign_matches_player_id ON campaign_matches (player_id);
