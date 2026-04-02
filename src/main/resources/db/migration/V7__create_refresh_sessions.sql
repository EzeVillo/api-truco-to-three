CREATE TABLE refresh_sessions
(
    id                     UUID PRIMARY KEY,
    user_id                UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash             VARCHAR(64) NOT NULL UNIQUE,
    expires_at             TIMESTAMPTZ NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL,
    revoked_at             TIMESTAMPTZ,
    rotated_at             TIMESTAMPTZ,
    replaced_by_session_id UUID REFERENCES refresh_sessions (id)
);

CREATE INDEX idx_refresh_sessions_user_id ON refresh_sessions (user_id);
CREATE INDEX idx_refresh_sessions_replaced_by ON refresh_sessions (replaced_by_session_id) WHERE replaced_by_session_id IS NOT NULL;
