ALTER TABLE matches
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

DROP INDEX IF EXISTS idx_matches_invite_code;
CREATE UNIQUE INDEX uq_matches_invite_code_not_null
    ON matches (invite_code) WHERE invite_code IS NOT NULL;
CREATE INDEX idx_matches_public_waiting
    ON matches (last_activity_at DESC) WHERE visibility = 'PUBLIC' AND status = 'WAITING_FOR_PLAYERS';

ALTER TABLE leagues
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE leagues
    ALTER COLUMN invite_code DROP NOT NULL;

DROP INDEX IF EXISTS idx_leagues_invite_code;
CREATE UNIQUE INDEX uq_leagues_invite_code_not_null
    ON leagues (invite_code) WHERE invite_code IS NOT NULL;
CREATE INDEX idx_leagues_public_waiting
    ON leagues (last_activity_at DESC) WHERE visibility = 'PUBLIC' AND status = 'WAITING_FOR_PLAYERS';

ALTER TABLE cups
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE cups
    ALTER COLUMN invite_code DROP NOT NULL;

DROP INDEX IF EXISTS idx_cups_invite_code;
CREATE UNIQUE INDEX uq_cups_invite_code_not_null
    ON cups (invite_code) WHERE invite_code IS NOT NULL;
CREATE INDEX idx_cups_public_waiting
    ON cups (last_activity_at DESC) WHERE visibility = 'PUBLIC' AND status = 'WAITING_FOR_PLAYERS';
