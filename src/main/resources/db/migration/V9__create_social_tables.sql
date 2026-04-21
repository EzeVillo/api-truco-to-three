CREATE TABLE social_friendships
(
    id           UUID PRIMARY KEY,
    requester_id UUID        NOT NULL,
    addressee_id UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    version      INT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_social_friendships_pair
    ON social_friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

CREATE INDEX idx_social_friendships_requester
    ON social_friendships (requester_id);

CREATE INDEX idx_social_friendships_addressee_pending
    ON social_friendships (addressee_id) WHERE status = 'PENDING';

CREATE INDEX idx_social_friendships_accepted_by_player
    ON social_friendships (requester_id, addressee_id) WHERE status = 'ACCEPTED';

CREATE TABLE social_resource_invitations
(
    id           UUID PRIMARY KEY,
    sender_id    UUID        NOT NULL,
    recipient_id UUID        NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    target_id    UUID        NOT NULL,
    invite_code  VARCHAR(8)  NOT NULL,
    status       VARCHAR(20) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    version      INT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_social_resource_invitations_pending
    ON social_resource_invitations (sender_id, recipient_id, target_type, target_id) WHERE status = 'PENDING';

CREATE INDEX idx_social_resource_invitations_recipient_pending
    ON social_resource_invitations (recipient_id, expires_at) WHERE status = 'PENDING';

CREATE INDEX idx_social_resource_invitations_pending_expiration
    ON social_resource_invitations (expires_at) WHERE status = 'PENDING';
