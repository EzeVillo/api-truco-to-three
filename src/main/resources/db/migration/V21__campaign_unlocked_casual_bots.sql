CREATE TABLE campaign_unlocked_casual_bots
(
    player_id UUID NOT NULL REFERENCES campaign_progress (player_id) ON DELETE CASCADE,
    bot_id    UUID NOT NULL,
    PRIMARY KEY (player_id, bot_id)
);

ALTER TABLE campaign_progress
    ADD COLUMN all_casual_bots_unlocked BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill retroactivo: el desbloqueo solo se computa al ganar un challenge nuevo,
-- asi que los rivales que ya tenian net (wins - losses) >= 3 antes de este cambio
-- no quedarian desbloqueados. Los sembramos desde el historial existente.
-- NOTA: a proposito NO se otorga el logro UNLOCK_ALL_CAMPAIGN_BOTS_IN_CASUAL aca;
-- ese sigue ganandose jugando (se dispara via evento de dominio, no por backfill).
INSERT INTO campaign_unlocked_casual_bots (player_id, bot_id)
SELECT player_id, rival_id
FROM campaign_rival_records
WHERE wins - losses >= 3 ON CONFLICT (player_id, bot_id) DO NOTHING;

-- Marca el flag para quien ya tenga los 100 bots del ladder desbloqueados.
UPDATE campaign_progress cp
SET all_casual_bots_unlocked = TRUE
WHERE (SELECT COUNT(*)
       FROM campaign_unlocked_casual_bots ub
       WHERE ub.player_id = cp.player_id) >= 100;
