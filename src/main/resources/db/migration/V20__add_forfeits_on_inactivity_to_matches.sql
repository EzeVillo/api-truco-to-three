ALTER TABLE matches
    ADD COLUMN forfeits_on_inactivity BOOLEAN NOT NULL DEFAULT TRUE;
