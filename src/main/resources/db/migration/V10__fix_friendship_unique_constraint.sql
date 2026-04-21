-- Drop the unconditional unique index that prevented re-sending friend requests
-- after a previous one was CANCELLED or DECLINED
DROP INDEX uk_social_friendships_pair;

-- Recreate it as a partial index on PENDING status only,
-- mirroring the pattern used by social_resource_invitations
CREATE UNIQUE INDEX uk_social_friendships_pair_pending
    ON social_friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) WHERE status = 'PENDING';
