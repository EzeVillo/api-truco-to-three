CREATE TABLE social_preferences
(
    player_id               UUID PRIMARY KEY,
    accepts_friend_requests BOOLEAN NOT NULL DEFAULT TRUE,
    version                 INT     NOT NULL DEFAULT 0
);
