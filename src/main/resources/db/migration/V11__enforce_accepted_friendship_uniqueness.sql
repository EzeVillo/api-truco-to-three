-- Keep a single active friendship per pair and demote duplicate ACCEPTED rows
-- so the new partial unique index can be created safely.
WITH ranked_accepted_friendships AS (SELECT id,
                                            ROW_NUMBER() OVER (
               PARTITION BY LEAST(requester_id, addressee_id),
                            GREATEST(requester_id, addressee_id)
               ORDER BY id
           ) AS pair_rank
                                     FROM social_friendships
                                     WHERE status = 'ACCEPTED')
UPDATE social_friendships friendship
SET status  = 'REMOVED',
    version = friendship.version + 1 FROM ranked_accepted_friendships ranked
WHERE friendship.id = ranked.id
  AND ranked.pair_rank
    > 1;

CREATE UNIQUE INDEX uk_social_friendships_pair_accepted
    ON social_friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id)) WHERE status = 'ACCEPTED';
