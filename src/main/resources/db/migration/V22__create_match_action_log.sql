CREATE TABLE match_action_log
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_id       UUID        NOT NULL,
    state_version  BIGINT      NOT NULL,
    game_number    INT         NOT NULL,
    round_number   INT         NOT NULL,
    actor_seat     VARCHAR(16) NOT NULL,
    actor_type     VARCHAR(8)  NOT NULL,
    action_type    VARCHAR(24) NOT NULL,
    action_detail  JSONB,
    match_state    JSONB       NOT NULL,
    schema_version INT         NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL,
    recorded_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_match_action_log_match_state_version UNIQUE (match_id, state_version)
);
