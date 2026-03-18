ALTER TABLE tournaments RENAME TO leagues;
ALTER TABLE tournament_participants RENAME TO league_participants;
ALTER TABLE tournament_fixtures RENAME TO league_fixtures;
ALTER TABLE tournament_wins RENAME TO league_wins;

ALTER TABLE league_participants RENAME COLUMN tournament_id TO league_id;
ALTER TABLE league_fixtures RENAME COLUMN tournament_id TO league_id;
ALTER TABLE league_wins RENAME COLUMN tournament_id TO league_id;

ALTER
INDEX idx_tournaments_invite_code RENAME TO idx_leagues_invite_code;
ALTER
INDEX idx_fixtures_tournament RENAME TO idx_fixtures_league;
