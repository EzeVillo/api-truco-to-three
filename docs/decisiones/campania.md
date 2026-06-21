# Modo Campaña

> Decisión de dominio no obvia. Verificá contra el estado actual del código antes de asumir que
> sigue vigente.

Bounded context `com.villo.truco.campaign`, modelado calcando `profile`/`rematch`. Single-player: el
jugador sube un ranking fijo de 100 bots (curva cuadrática, cima ≈ 43.200 pts ≈ 24 h de juego) hasta
el #1, desafiando siempre al inmediato superior. Alcanzado el #1, puede desafiar a cualquiera. Todos
los cruces son al **mejor de 5**, sin revancha.

**Fórmulas (en dominio, no en infra):**

- Puntos por victoria: `100 × (gamesGanador − gamesPerdedor)` (`CampaignScoringPolicy`). Derrota =
  0,
  puntos nunca negativos (`CampaignPoints`).
- Distribución de bots: `CampaignPointsCurve.pointsForPosition` =
  `round5(topPoints × ((total+1−pos)/total)²)`. La posición se deriva de puntos: para superar a un
  bot hay que tener **estrictamente más** (empate no alcanza) → `CampaignLadder.positionFor` /
  `nextRival`.

**Agregado `CampaignProgress`** (uno por jugador, key = `PlayerId`): puntos, `activeChallenge` (un
solo desafío activo a la vez), head-to-head `rivalRecords`, flags `topOneReached` /
`allRivalsDefeated`. Emite `CampaignChallengeStarted/Won/Lost`, `CampaignTopOneReached`,
`CampaignAllRivalsDefeated`.

**Flujo:** `StartCampaignChallengeCommandHandler` valida rival desafiable y delega la creación del
match en `CreateBotMatchUseCase` (reutiliza el flujo de bot match con `MatchRules` de mejor de 5),
registra el match en `CampaignMatchRegistry`. `CampaignChallengeResolutionService` (handler
`MatchDomainEvent` registrado en el dispatcher de match de `EventNotifierConfiguration`) reacciona a
`MatchFinished/Forfeited/Abandoned`, acredita puntos y emite eventos.

**Logros (en `profile`):** `REACH_CAMPAIGN_TOP_ONE` y `DEFEAT_ALL_CAMPAIGN_RIVALS` (ganar ≥1 vez a
los 100; subir el ranking puede saltear bots, así que exige volver tras el #1).
`ProfileCampaignDomainEventHandler` escucha eventos de campaign — el head-to-head es la base del
logro de ganarle a todos.

**Integraciones cross-context:**

- `CampaignRematchVeto` implementa el puerto `application.ports.RematchVeto`, por lo que los matches
  de campaña NO generan `RematchSession`.
- `GetBotsQueryHandler` filtra los 100 bots de campaña del catálogo casual `GET /api/bots` (recibe
  `Set<PlayerId>` de hidden bots desde `CampaignBotCatalog`).

**Catálogo:** `CampaignBotCatalog` (determinista, IDs `c0000000-...-NNN`, personalidad escalada por
franja) implementa `CampaignLadderProvider` y registra los bots en el `BotRegistry` vía
`CampaignBotCatalogInitializer`.

**Endpoints:** `GET /api/campaign` (ranking + progreso) y `POST /api/campaign/challenges` (body
opcional `botId`, solo válido tras el #1). Tabla Flyway `V19__create_campaign_tables.sql`
(`campaign_progress`, `campaign_rival_records`, `campaign_matches`).

## Desbloqueo de bots de campaña para el modo casual

Ganar en campaña desbloquea bots de campaña para jugarlos en el **modo casual** (`GET /api/bots`).

**Reglas de dominio (en `CampaignProgress`):**

- Umbral = **diferencia neta `wins − losses >= 3`** contra un rival
  (`CampaignRivalRecord.net()` / `isFavorableBy`).
- Desbloqueo = **latch permanente**: `Set<PlayerId> unlockedCasualBots` (solo crece). Se evalúa
  **solo en `resolveChallengeWon`** (`evaluateCasualUnlocks`); una derrota nunca revierte aunque el
  neto baje.
- Eventos: `CampaignBotUnlockedForCasualEvent` (por bot) y `AllCampaignBotsUnlockedForCasualEvent`
  (al desbloquear los 100, idempotente vía flag `allCasualBotsUnlocked`, mismo molde que
  `topOneReached` / `allRivalsDefeated`).
- Las **partidas casuales NO afectan** el head-to-head de campaña (un casual nunca crea
  `activeChallenge`).

**Logro:** `UNLOCK_ALL_CAMPAIGN_BOTS_IN_CASUAL` (en `profile`), disparado por
`AllCampaignBotsUnlockedForCasualEvent` en `ProfileCampaignAchievementService`. Más difícil que
`DEFEAT_ALL_CAMPAIGN_RIVALS`.

**`GET /api/bots` cambió de contrato (breaking):** antes array plano global; ahora **player-scoped**
y devuelve `{ casual, campaignUnlocked }` (`BotCatalogResponse` / `BotCatalogDTO`). Nuevo puerto
`application.ports.RevealedBotIdsProvider` (impl `CampaignRevealedBotIdsProvider`), inyectado junto
a `HiddenBotIdsProvider` en `GetBotsQueryHandler`. Los bots de campaña siguen ocultos del `casual`;
aparecen en `campaignUnlocked` solo los revelados de ese jugador.

**WS:** cada desbloqueo emite `CAMPAIGN_BOT_UNLOCKED` (`{ botId, matchId }`) por
`/user/queue/campaign` (`CampaignNotificationEventTranslator`).

**Persistencia:** Flyway `V20__campaign_unlocked_casual_bots.sql` (tabla
`campaign_unlocked_casual_bots` + columna `all_casual_bots_unlocked` en `campaign_progress`).
`@ElementCollection` en `CampaignProgressJpaEntity`; el snapshot/rehydrator de `CampaignProgress`
llevan `unlockedCasualBots` + `allCasualBotsUnlocked`. El contract test
`AchievementCatalogContractTest` exige que la sección 8.3 de `docs/CONTRATOS_API.md` liste
exactamente el enum `AchievementCode`.
