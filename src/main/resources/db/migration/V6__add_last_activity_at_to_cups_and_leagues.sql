ALTER TABLE cups
    ADD COLUMN last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX idx_cups_idle
    ON cups (last_activity_at) WHERE status IN ('IN_PROGRESS', 'READY', 'WAITING_FOR_PLAYERS');

ALTER TABLE leagues
    ADD COLUMN last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX idx_leagues_idle
    ON leagues (last_activity_at) WHERE status IN ('IN_PROGRESS', 'READY', 'WAITING_FOR_PLAYERS');
