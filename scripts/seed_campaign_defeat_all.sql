-- ============================================================================
-- ESCENARIO 1: DEFEAT_ALL_CAMPAIGN_RIVALS
-- Quedás top-1, con victoria head-to-head contra TODOS menos el ultimo bot.
-- Luego, desde la app, desafias y ganas al bot de la posicion 100 -> salta el logro.
-- ============================================================================
--
-- IMPORTANTE: el logro NO se desbloquea por estado de BD; se dispara por el
-- EVENTO de dominio cuando resolves (ganas) el ultimo desafio. Este script deja
-- el estado JUSTO ANTES de esa victoria. Vos tenes que jugar y ganar ese match.
--
-- ============================================================================

DO
$$
DECLARE
v_player UUID := '19be4742-e311-4a1f-bf4b-b34e145e4d05';
BEGIN
  -- Progreso: top-1 ya alcanzado (43205 > 43200 del bot #1 => posicion 1),
  -- sin desafio activo, all_rivals_defeated todavia en FALSE.
INSERT INTO campaign_progress (player_id, points, active_challenge_match_id,
                               active_challenge_rival_id, top_one_reached,
                               all_rivals_defeated, version)
VALUES (v_player, 43205, NULL, NULL, TRUE, FALSE, 0) ON CONFLICT (player_id) DO
UPDATE
    SET points = 43205,
    active_challenge_match_id = NULL,
    active_challenge_rival_id = NULL,
    top_one_reached = TRUE,
    all_rivals_defeated = FALSE;

-- Limpiar head-to-head previo y sembrar 1 victoria contra los bots #1..#99.
-- Falta SOLO el bot #100 (c0000000-...-000000000100): ese lo ganas en la app.
DELETE
FROM campaign_rival_records
WHERE player_id = v_player;

INSERT INTO campaign_rival_records (player_id, rival_id, wins, losses)
SELECT v_player,
       ('c0000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid, 1,
       0
FROM generate_series(1, 99) AS n;

-- Opcional: si ya tenias el logro desbloqueado de antes, descomenta para
-- forzar que vuelva a emitirse la notificacion al ganar.
-- DELETE FROM player_achievements
--  WHERE player_id = v_player AND achievement_code = 'DEFEAT_ALL_CAMPAIGN_RIVALS';
END $$;

-- Despues de correr esto, en la app (estando top-1):
--   POST /api/campaign/challenges  { "botId": "c0000000-0000-0000-0000-000000000100" }
-- Jugas el mejor-de-5, ganas, y al resolverse salta DEFEAT_ALL_CAMPAIGN_RIVALS.
