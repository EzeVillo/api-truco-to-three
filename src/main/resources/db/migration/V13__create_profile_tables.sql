CREATE TABLE player_profiles
(
    player_id UUID PRIMARY KEY,
    version   INT NOT NULL DEFAULT 0
);

CREATE TABLE player_achievements
(
    player_id        UUID         NOT NULL REFERENCES player_profiles (player_id) ON DELETE CASCADE,
    achievement_code VARCHAR(100) NOT NULL,
    unlocked_at      TIMESTAMPTZ  NOT NULL,
    match_id         UUID         NOT NULL,
    game_number      INT          NOT NULL,
    PRIMARY KEY (player_id, achievement_code)
);

CREATE INDEX idx_player_achievements_player_unlocked_at
    ON player_achievements (player_id, unlocked_at DESC);

CREATE TABLE match_achievement_trackers
(
    match_id       UUID PRIMARY KEY,
    player_one_id  UUID    NOT NULL,
    player_two_id  UUID    NOT NULL,
    human_vs_human BOOLEAN NOT NULL,
    state          JSONB   NOT NULL,
    version        INT     NOT NULL DEFAULT 0
);
