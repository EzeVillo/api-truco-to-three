# Logros de envido con "WIN_MATCH": van en SCORE_CHANGED, no en ENVIDO_RESOLVED

> Decisión de dominio no obvia. Verificá contra el estado actual del código antes de asumir que
> sigue vigente.

Los logros que dicen "WIN_MATCH" pero se ganan vía envido deben evaluarse en la rama `SCORE_CHANGED`
de `AchievementPolicy`, **no** en `ENVIDO_RESOLVED`.

**Por qué:** el orden real de emisión en Match es `EnvidoResolvedEvent → ScoreChangedEvent →
MatchFinishedEvent`. El `ProfileAchievementTrackingService` procesa cada evento de forma
independiente, por lo que en `ENVIDO_RESOLVED` el score todavía es el **previo** al envido y el
tracker nunca tenía el score final. Funcionaba por casualidad, pero era un acoplamiento implícito al
scoring.

**Cómo aplicar:** usar `previousScore` para validar el estado pre-envido y el guard
`winnerSeat == lastEnvidoWinnerSeat` para confirmar que el `SCORE_CHANGED` provino del envido. El
contexto del envido (`lastEnvidoWinnerSeat`, `lastEnvidoPointsMano/Pie`, `envidoCallsInRound`)
sobrevive hasta el siguiente `onRoundStarted`, por lo que está disponible en `SCORE_CHANGED`.
`MatchFinishedEvent` cae en default → OTHER y es ignorado por la policy.

**Fix aplicado (2026-05-14):** los tres checks fueron movidos a `resolveScoreAchievements` (rama
`SCORE_CHANGED`). Logros afectados:

- `WIN_MATCH_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2`
- `WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2`
- `WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO`
