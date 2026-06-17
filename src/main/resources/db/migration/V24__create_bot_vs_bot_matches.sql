CREATE TABLE bot_vs_bot_matches
(
    match_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL
);

CREATE INDEX idx_bot_vs_bot_matches_owner_id ON bot_vs_bot_matches (owner_id);
