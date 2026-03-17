CREATE TABLE users
(
    id              UUID PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL
);

CREATE TABLE matches
(
    id                   UUID PRIMARY KEY,
    player_one           UUID        NOT NULL,
    player_two           UUID,
    invite_code          VARCHAR(8),
    status               VARCHAR(30) NOT NULL,
    games_to_win         INT         NOT NULL,
    games_won_player_one INT         NOT NULL DEFAULT 0,
    games_won_player_two INT         NOT NULL DEFAULT 0,
    game_number          INT         NOT NULL DEFAULT 0,
    score_player_one     INT         NOT NULL DEFAULT 0,
    score_player_two     INT         NOT NULL DEFAULT 0,
    round_number         INT         NOT NULL DEFAULT 0,
    ready_player_one     BOOLEAN     NOT NULL DEFAULT FALSE,
    ready_player_two     BOOLEAN     NOT NULL DEFAULT FALSE,
    first_mano_of_game   UUID,
    current_round        JSONB,
    version              INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_matches_invite_code ON matches (invite_code) WHERE invite_code IS NOT NULL;
CREATE INDEX idx_matches_active_p1 ON matches (player_one) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_matches_active_p2 ON matches (player_two) WHERE status = 'IN_PROGRESS';

CREATE TABLE tournaments
(
    id            UUID PRIMARY KEY,
    capacity      INT         NOT NULL,
    games_to_play INT         NOT NULL,
    invite_code   VARCHAR(8)  NOT NULL,
    status        VARCHAR(30) NOT NULL,
    version       INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_tournaments_invite_code ON tournaments (invite_code);

CREATE TABLE tournament_participants
(
    tournament_id UUID NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    player_id     UUID NOT NULL,
    ordinal       INT  NOT NULL,
    PRIMARY KEY (tournament_id, player_id)
);

CREATE TABLE tournament_fixtures
(
    id              UUID PRIMARY KEY,
    tournament_id   UUID        NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    matchday_number INT         NOT NULL,
    player_one      UUID,
    player_two      UUID,
    match_id        UUID,
    winner          UUID,
    status          VARCHAR(20) NOT NULL
);

CREATE INDEX idx_fixtures_tournament ON tournament_fixtures (tournament_id);

CREATE TABLE tournament_wins
(
    tournament_id UUID NOT NULL REFERENCES tournaments (id) ON DELETE CASCADE,
    player_id     UUID NOT NULL,
    wins          INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (tournament_id, player_id)
);
