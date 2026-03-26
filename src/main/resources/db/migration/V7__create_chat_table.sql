CREATE TABLE chats
(
    id                      UUID PRIMARY KEY,
    parent_type             VARCHAR(10) NOT NULL,
    parent_id               VARCHAR(36) NOT NULL,
    participants            JSONB       NOT NULL,
    messages                JSONB       NOT NULL DEFAULT '[]',
    last_message_timestamps JSONB       NOT NULL DEFAULT '{}',
    rate_limit_ms           BIGINT      NOT NULL DEFAULT 2000,
    version                 INT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_chats_parent ON chats (parent_type, parent_id);
