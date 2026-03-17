ALTER TABLE matches
    ADD COLUMN last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX idx_matches_idle
    ON matches (last_activity_at) WHERE status IN ('IN_PROGRESS', 'READY');
