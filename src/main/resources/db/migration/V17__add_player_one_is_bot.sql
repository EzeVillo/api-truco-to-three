ALTER TABLE rematch_sessions
    ADD COLUMN player_one_is_bot BOOLEAN NOT NULL DEFAULT FALSE;
