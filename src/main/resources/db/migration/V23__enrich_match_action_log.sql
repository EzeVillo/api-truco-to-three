ALTER TABLE match_action_log
    RENAME COLUMN match_state TO match_state_before;

ALTER TABLE match_action_log
    ADD COLUMN match_state_after  JSONB,
    ADD COLUMN decision_context   JSONB,
    ADD COLUMN score_actor_before INT     NOT NULL DEFAULT 0,
    ADD COLUMN score_actor_after  INT     NOT NULL DEFAULT 0,
    ADD COLUMN score_opp_before   INT     NOT NULL DEFAULT 0,
    ADD COLUMN score_opp_after    INT     NOT NULL DEFAULT 0,
    ADD COLUMN tantos_actor       INT     NOT NULL DEFAULT 0,
    ADD COLUMN tantos_opp         INT     NOT NULL DEFAULT 0,
    ADD COLUMN is_mano            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN forced             BOOLEAN NOT NULL DEFAULT FALSE;
