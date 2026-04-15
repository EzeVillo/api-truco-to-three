ALTER TABLE matches
    RENAME COLUMN invite_code TO join_code;

ALTER TABLE leagues
    RENAME COLUMN invite_code TO join_code;

ALTER TABLE cups
    RENAME COLUMN invite_code TO join_code;

DROP INDEX IF EXISTS idx_matches_invite_code;
DROP INDEX IF EXISTS uq_matches_invite_code_not_null;
DROP INDEX IF EXISTS idx_leagues_invite_code;
DROP INDEX IF EXISTS uq_leagues_invite_code_not_null;
DROP INDEX IF EXISTS idx_cups_invite_code;
DROP INDEX IF EXISTS uq_cups_invite_code_not_null;

CREATE
TEMP TABLE tmp_join_code_registry
(
    join_code   VARCHAR(8) PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID        NOT NULL
) ON COMMIT DROP;

DO
$$
    DECLARE
rec       RECORD;
        candidate
VARCHAR(8);
        attempts
INT;
BEGIN
FOR rec IN
SELECT id, join_code
FROM matches
ORDER BY id LOOP
                candidate := rec.join_code;

IF
candidate IS NULL
                    OR EXISTS (SELECT 1
                               FROM tmp_join_code_registry
                               WHERE join_code = candidate) THEN
                    attempts := 0;
                    LOOP
attempts := attempts + 1;
                        candidate
:= UPPER(SUBSTRING(MD5(
                                'MATCH:' || rec.id::TEXT || ':' || CLOCK_TIMESTAMP()::TEXT || ':'
                                    || RANDOM()::TEXT || ':' || attempts::TEXT) FROM 1 FOR 8));
                        EXIT
WHEN NOT EXISTS (SELECT 1
                                              FROM tmp_join_code_registry
                                              WHERE join_code = candidate);
END LOOP;

UPDATE matches
SET join_code = candidate
WHERE id = rec.id;
END IF;

INSERT INTO tmp_join_code_registry (join_code, target_type, target_id)
VALUES (candidate, 'MATCH', rec.id);
END LOOP;

FOR rec IN
SELECT id, join_code
FROM leagues
ORDER BY id LOOP
                candidate := rec.join_code;

IF
candidate IS NULL
                    OR EXISTS (SELECT 1
                               FROM tmp_join_code_registry
                               WHERE join_code = candidate) THEN
                    attempts := 0;
                    LOOP
attempts := attempts + 1;
                        candidate
:= UPPER(SUBSTRING(MD5(
                                'LEAGUE:' || rec.id::TEXT || ':' || CLOCK_TIMESTAMP()::TEXT || ':'
                                    || RANDOM()::TEXT || ':' || attempts::TEXT) FROM 1 FOR 8));
                        EXIT
WHEN NOT EXISTS (SELECT 1
                                              FROM tmp_join_code_registry
                                              WHERE join_code = candidate);
END LOOP;

UPDATE leagues
SET join_code = candidate
WHERE id = rec.id;
END IF;

INSERT INTO tmp_join_code_registry (join_code, target_type, target_id)
VALUES (candidate, 'LEAGUE', rec.id);
END LOOP;

FOR rec IN
SELECT id, join_code
FROM cups
ORDER BY id LOOP
                candidate := rec.join_code;

IF
candidate IS NULL
                    OR EXISTS (SELECT 1
                               FROM tmp_join_code_registry
                               WHERE join_code = candidate) THEN
                    attempts := 0;
                    LOOP
attempts := attempts + 1;
                        candidate
:= UPPER(SUBSTRING(MD5(
                                'CUP:' || rec.id::TEXT || ':' || CLOCK_TIMESTAMP()::TEXT || ':'
                                    || RANDOM()::TEXT || ':' || attempts::TEXT) FROM 1 FOR 8));
                        EXIT
WHEN NOT EXISTS (SELECT 1
                                              FROM tmp_join_code_registry
                                              WHERE join_code = candidate);
END LOOP;

UPDATE cups
SET join_code = candidate
WHERE id = rec.id;
END IF;

INSERT INTO tmp_join_code_registry (join_code, target_type, target_id)
VALUES (candidate, 'CUP', rec.id);
END LOOP;
END
$$;

ALTER TABLE matches
    ALTER COLUMN join_code SET NOT NULL;

ALTER TABLE leagues
    ALTER COLUMN join_code SET NOT NULL;

ALTER TABLE cups
    ALTER COLUMN join_code SET NOT NULL;

CREATE UNIQUE INDEX uq_matches_join_code
    ON matches (join_code);

CREATE UNIQUE INDEX uq_leagues_join_code
    ON leagues (join_code);

CREATE UNIQUE INDEX uq_cups_join_code
    ON cups (join_code);

CREATE TABLE join_code_registry
(
    join_code   VARCHAR(8) PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id   UUID        NOT NULL
);

INSERT INTO join_code_registry (join_code, target_type, target_id)
SELECT join_code, target_type, target_id
FROM tmp_join_code_registry;

ALTER TABLE social_resource_invitations
DROP
COLUMN invite_code;
