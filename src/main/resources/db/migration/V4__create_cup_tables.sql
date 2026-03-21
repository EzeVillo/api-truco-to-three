CREATE TABLE cups
(
    id                UUID PRIMARY KEY,
    number_of_players INT         NOT NULL,
    games_to_play     INT         NOT NULL,
    invite_code       VARCHAR(8)  NOT NULL,
    status            VARCHAR(30) NOT NULL,
    champion          UUID,
    version           INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_cups_invite_code ON cups (invite_code);

CREATE TABLE cup_participants
(
    cup_id    UUID NOT NULL REFERENCES cups (id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    ordinal   INT  NOT NULL,
    PRIMARY KEY (cup_id, player_id)
);

CREATE TABLE cup_bouts
(
    id               UUID PRIMARY KEY,
    cup_id           UUID        NOT NULL REFERENCES cups (id) ON DELETE CASCADE,
    round_number     INT         NOT NULL,
    bracket_position INT         NOT NULL,
    player_one       UUID,
    player_two       UUID,
    match_id         UUID,
    winner           UUID,
    status           VARCHAR(20) NOT NULL
);

CREATE INDEX idx_cup_bouts_cup ON cup_bouts (cup_id);
CREATE INDEX idx_cup_bouts_match ON cup_bouts (match_id) WHERE match_id IS NOT NULL;

CREATE TABLE cup_forfeited_players
(
    cup_id    UUID NOT NULL REFERENCES cups (id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    PRIMARY KEY (cup_id, player_id)
);
