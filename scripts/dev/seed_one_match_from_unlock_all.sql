-- =============================================================================
-- DEV ONLY (PostgreSQL) — Deja a un jugador a UNA partida ganada de desbloquear
-- todos los bots de campania en casual y disparar el logro
-- UNLOCK_ALL_CAMPAIGN_BOTS_IN_CASUAL.
--
-- Contexto del modelo:
--   - El ladder tiene 100 bots: ids 'c0000000-0000-0000-0000-<posicion 12 digitos>'
--     (posicion 1..100). El bot 100 es el ultimo (mas debil).
--   - Un bot se desbloquea para el casual cuando net (wins - losses) >= 3 contra ese
--     rival. El calculo se dispara SOLO al GANAR un challenge nuevo (no es retroactivo).
--   - Al llegar a 100 desbloqueados -> AllCampaignBotsUnlockedForCasualEvent -> logro.
--   - IMPORTANTE: solo se puede desafiar al rival inmediatamente por encima... EXCEPTO
--     si ya alcanzaste el top-1 (ensureCanChallenge corta temprano con top_one_reached).
--     Por eso seteamos top_one_reached = TRUE, asi se puede desafiar al bot 100 directo.
--
-- Estrategia: 99 bots (pos 1..99) ya desbloqueados, flag en FALSE, y el bot 100 con
-- wins=2/losses=0 (net=2). Al iniciar challenge contra el bot 100 y GANARLO -> net=3 ->
-- se desbloquea el ultimo -> size==100 -> logro.
--
-- USO: editar el UUID en la unica linea marcada (-- <<< EDITAR) y ejecutar todo el
-- bloque tal cual en cualquier cliente Postgres (psql, DBeaver, IntelliJ, pgAdmin...).
-- Es un unico DO $$...$$ en SQL plano: no usa meta-comandos de psql (nada de \set ni :var).
-- =============================================================================

DO
$$
DECLARE
target_player UUID := '00000000-0000-0000-0000-000000000000';  -- <<< EDITAR: UUID del player
    last_bot
UUID := 'c0000000-0000-0000-0000-000000000100';  -- bot de posicion 100
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM campaign_progress WHERE player_id = target_player) THEN
        -- No abortamos la transaccion: solo avisamos y salimos sin tocar nada.
        RAISE NOTICE 'No existe campaign_progress para el player % — no se hizo nada.',
            target_player;
        RETURN;
END IF;

    -- 1) Habilita desafiar a cualquier bot (top-1) y limpia challenge activo + flag.
UPDATE campaign_progress
SET top_one_reached           = TRUE,
    all_casual_bots_unlocked  = FALSE,
    active_challenge_match_id = NULL,
    active_challenge_rival_id = NULL
WHERE player_id = target_player;

-- 2) Reset de desbloqueos y carga de los bots de posicion 1..99.
DELETE
FROM campaign_unlocked_casual_bots
WHERE player_id = target_player;

INSERT INTO campaign_unlocked_casual_bots (player_id, bot_id)
SELECT target_player,
       ('c0000000-0000-0000-0000-' || lpad(g::text, 12, '0')) ::uuid
FROM generate_series(1, 99) AS g;

-- 3) Bot 100: net=2 (wins=2, losses=0). Una victoria mas lo lleva a net=3.
INSERT INTO campaign_rival_records (player_id, rival_id, wins, losses)
VALUES (target_player, last_bot, 2, 0) ON CONFLICT (player_id, rival_id) DO
UPDATE SET wins = 2, losses = 0;

RAISE
NOTICE 'Listo: player % queda a 1 victoria del logro (99 bots desbloqueados).',
        target_player;
END $$;

-- Verificacion (reemplaza el UUID):
--   SELECT count(*) FROM campaign_unlocked_casual_bots
--   WHERE player_id = '00000000-0000-0000-0000-000000000000';  -- 99
--   SELECT all_casual_bots_unlocked, top_one_reached FROM campaign_progress
--   WHERE player_id = '00000000-0000-0000-0000-000000000000';  -- f, t
-- Ahora: inicia un challenge de campania contra el bot de posicion 100 y ganalo ->
-- deberia llegar CAMPAIGN_BOT_UNLOCKED y desbloquearse el logro.
