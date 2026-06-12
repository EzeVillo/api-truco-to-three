-- ============================================================================
-- ESCENARIO 2: REACH_CAMPAIGN_TOP_ONE
-- Quedas en la posicion 2, a 1 punto de superar al #1.
-- Luego, desde la app, desafias al rival inmediato superior (el bot #1) y ganas
-- -> pasas a la posicion 1 -> salta el logro.
-- ============================================================================
--
-- IMPORTANTE: el logro se dispara por el EVENTO de dominio al ganar el desafio,
-- no por el estado de BD. Este script deja el estado JUSTO ANTES de esa victoria.
--
-- Detalle de puntos:
--   bot #1 = 43200 pts. Con 43200 pts el jugador queda en posicion 2 (empate no
--   alcanza: hay que tener ESTRICTAMENTE mas). Para superarlo faltan 1 pto, y la
--   minima victoria otorga 100 -> 43300 > 43200 -> posicion 1 -> TOP_ONE.
--
-- ============================================================================

DO
$$
DECLARE
v_player UUID := '19be4742-e311-4a1f-bf4b-b34e145e4d05';
BEGIN
  -- Progreso: 43200 pts => posicion 2, a 1 pto del #1, top_one_reached = FALSE.
INSERT INTO campaign_progress (player_id, points, active_challenge_match_id,
                               active_challenge_rival_id, top_one_reached,
                               all_rivals_defeated, version)
VALUES (v_player, 43200, NULL, NULL, FALSE, FALSE, 0) ON CONFLICT (player_id) DO
UPDATE
    SET points = 43200,
    active_challenge_match_id = NULL,
    active_challenge_rival_id = NULL,
    top_one_reached = FALSE,
    all_rivals_defeated = FALSE;

-- Sin head-to-head: asi solo salta TOP_ONE (no DEFEAT_ALL) al ganar.
DELETE
FROM campaign_rival_records
WHERE player_id = v_player;

-- Opcional: si ya tenias el logro desbloqueado de antes, descomenta para
-- forzar que vuelva a emitirse la notificacion al ganar.
-- DELETE FROM player_achievements
--  WHERE player_id = v_player AND achievement_code = 'REACH_CAMPAIGN_TOP_ONE';
END $$;

-- Despues de correr esto, en la app (posicion 2):
--   POST /api/campaign/challenges   (sin body / sin botId)
-- Toma automaticamente al rival inmediato superior (bot #1). Jugas, ganas,
-- y al resolverse salta REACH_CAMPAIGN_TOP_ONE.
