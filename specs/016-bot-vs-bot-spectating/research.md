# Research / Decisiones de diseño: Partidas Bot vs Bot Espectables

Documenta las decisiones no obvias del plan. Validadas contra el estado actual del código
(2026-06-16).

## D1 — Ocupación por autoría, no por espectado

**Decisión**: la ocupación del creador deriva de **ser dueño** de la partida bot-vs-bot, persistido
en un registro. No se usa `Spectatorship` como mecanismo de ocupación.

**Rationale**: `Spectatorship` se desactiva cuando el espectador cierra su última sesión WS (ver
`docs/CONTRATOS_API.md` §4.16: "deja de ser espectador solo cuando se desconecta la última sesión").
Si la ocupación dependiera de eso, el usuario podría crear una partida, cerrar la pestaña y crear
otra → partidas infinitas, justo lo que el pedido quiere evitar ("generar el match te deja ocupado,
por más que no lo veas").

**Alternativas consideradas**:
- *Spectatorship como ocupación* (iteración previa): rechazada por lo anterior.
- *Campo `ownerId`/`createdBy` en el agregado `Match`*: rechazada por YAGNI/pureza — obligaría a
  tocar `Match`, su entidad JPA, el mapper, `reconstruct`, todas las factories y una migración de la
  tabla `matches`. El registro lateral es mucho menos invasivo.

## D2 — Registro de autoría `BotVsBotMatchRegistry` (espeja a campaña)

**Decisión**: nuevo puerto de dominio `com.villo.truco.domain.ports.BotVsBotMatchRegistry` con tabla
propia `bot_vs_bot_matches (match_id PK, owner_id, idx owner_id)`, **espejando el patrón existente
`CampaignMatchRegistry`** (`campaign_matches`, `JpaCampaignMatchRegistryAdapter`).

**Puerto**:
```java
public interface BotVsBotMatchRegistry {
  void register(MatchId matchId, PlayerId ownerId);
  boolean isBotVsBotMatch(MatchId matchId);                 // existsById — veto + manos + eligibility
  Optional<PlayerId> findOwnerByMatchId(MatchId matchId);   // eligibility owner-only
  Optional<MatchId> findActiveOwnedMatchId(PlayerId ownerId); // ocupación/presencia (join con estado)
}
```

**Rationale**: una sola pieza nueva sirve a los cuatro consumidores (ocupación, presencia,
eligibility owner-only, veto de revancha). Patrón ya probado en el repo.

**"Activo"**: `findActiveOwnedMatchId` devuelve el match propio **sin finalizar**. El adapter JPA lo
resuelve con una query que cruza `bot_vs_bot_matches` con `matches` filtrando estados terminales
(`FINISHED`, `FORFEITED`, `ABANDONED`, `CANCELLED`). Como se bloquea crear una segunda mientras hay
una activa, devuelve a lo sumo una fila → O(1), sin escanear historial. No se borra la fila al
terminar (evita carreras de orden con el veto de revancha que también corre en `MatchFinished`); la
"actividad" se deriva del estado del match.

## D3 — Ocupación: `OWNS_BOT_MATCH` en `PlayerAvailabilityChecker` (busy total)

**Decisión**: agregar `BlockingReason.OWNS_BOT_MATCH` a `PlayerAvailabilityChecker.findBlockingReason`
(vía `botVsBotMatchRegistry.findActiveOwnedMatchId(player).isPresent()`), lanzando una nueva
`PlayerOwnsActiveBotMatchException` en `ensureAvailable`. Es **busy total**: bloquea crear otra
bot-match, partida normal, Quick Match, liga y copa (decisión del usuario).

**Rationale**: `ensureAvailable` ya es el punto único que protege todas las altas; sumar el motivo
ahí da consistencia automática. La guarda `if botRegistry.isBot(player) return empty` se mantiene
(los bots no se chequean; el creador es humano).

## D4 — Espectado owner-only en `SpectatingEligibilityPolicy`

**Decisión**: en `ensureCanStartWatching`, si `botVsBotMatchRegistry.isBotVsBotMatch(matchId)`:
- se conservan los chequeos de `IN_PROGRESS` y de "no espectar dos matches a la vez";
- se **omite** el requisito de amistad/competición;
- se exige que `findOwnerByMatchId(matchId) == spectatorId`; si no, se rechaza
  (`SpectateBotMatchNotOwnerException`, mapea a `422`/`SPECTATE_ERROR`).

El chequeo "no espectar tu propio match" pasa naturalmente: ambos asientos son bots y el creador no
es ninguno.

**Rationale**: realiza FR-008 (solo el creador) reutilizando el flujo de espectado existente
(WebSocket-first vía `/user/queue/match-spectate`). El registro de autoría es la fuente de verdad
del dueño. El dominio no depende de `BotRegistry` (puerto de application): usa el puerto de dominio
`BotVsBotMatchRegistry`, y `isBotVsBotMatch` ya implica "ambos asientos bots" por construcción.

## D5 — Vista con cartas de ambos bots

**Decisión**: agregar a `SpectatorRoundStateDTO` dos campos `handPlayerOne`/`handPlayerTwo`
(`List<CardDTO>`), poblados **solo** cuando el match es bot-vs-bot
(`botVsBotMatchRegistry.isBotVsBotMatch`). `SpectatorMatchStateDTOAssembler` los llena con
`match.getCardsOf(playerOne)` y `match.getCardsOf(playerTwo)` (cartas en mano restantes). Para
partidas con humanos quedan `null` → **el contrato existente no cambia**.

**Entrega en tiempo real**: el snapshot inicial `SPECTATE_STATE` y `GET /api/matches/{id}/spectate`
ya entregan ambas manos (al usar el assembler). Los eventos públicos en vivo (`CARD_PLAYED`,
`TRUCO_*`, `SCORE_CHANGED`, etc.) se siguen reenviando al espectador. Para refrescar las manos al
inicio de cada ronda nueva, el cliente **re-consulta** `GET /spectate` al recibir `ROUND_STARTED`.

**Alternativa rechazada**: reenviar al espectador los eventos privados por asiento (`HAND_DEALT` /
`PLAYER_HAND_UPDATED`) sin redactar para bot-vs-bot. Tocaría el pipeline de redacción del
`SpectatorNotificationEventTranslator` y rompería su invariante. YAGNI: exponer las manos en el
snapshot alcanza para el alcance backend; el refresh por ronda es responsabilidad del cliente.

## D6 — Sin revancha para bot-vs-bot

**Decisión**: `BotVsBotRematchVeto implements RematchVeto` con
`vetoesRematch(matchId) = botVsBotMatchRegistry.isBotVsBotMatch(matchId)`, agregado a la lista de
vetos que consume `RematchEligibilityPolicy` (Spring inyecta `List<RematchVeto>`).

**Rationale**: hoy `MatchFinishedRematchSessionCreator` abre revancha para todo match casual,
incluido bot-vs-bot, dejando una sesión huérfana (ningún humano para confirmar). El veto lo evita
(espeja `CampaignRematchVeto`).

## D7 — Creación: reusar `Match.createReady` y validar bots

**Decisión**: `CreateBotVsBotMatchCommandHandler`:
1. `playerAvailabilityChecker.ensureAvailable(ownerId)` (FR-004, incluye `OWNS_BOT_MATCH`).
2. validar que `botOneId != botTwoId` (FR-002) y que ambos están en `BotRegistry` (FR-003).
3. `Match.createReady(botOne, botTwo, MatchRules.fromGamesToPlay(gamesToPlay, false))`
   (`Visibility.PRIVATE` → fuera del lobby, FR-014).
4. `match.startMatch(botOne)` + `match.startMatch(botTwo)` → `IN_PROGRESS`.
5. `matchRepository.save(match)`; `botVsBotMatchRegistry.register(matchId, ownerId)` (FR-005).
6. `matchEventNotifier.publishDomainEvents(...)`; `clearDomainEvents()`.
7. devolver `matchId`.

Todo dentro del `retryTransactionalPipeline` (como `CreateBotMatchCommandHandler`). **No** se
auto-suscribe al creador como espectador: la ocupación es por autoría; si quiere ver, se suscribe por
WS (la eligibility owner-only lo habilita). El motor de bots juega solo
(`RoundStartedEvent → BotDomainEventTranslator → BotTurnRequired → AsyncBotActionExecutor`).

## D8 — Presencia: campo nuevo `ownedBotMatch`

**Decisión**: `UserPresenceResolver` suma una resolución `ownedBotMatch` vía
`botVsBotMatchRegistry.findActiveOwnedMatchId(player)` + estado del match
(`MatchQueryRepository`), produciendo `ActiveOwnedBotMatchRefDTO(matchId, status)`. `UserPresenceDTO`
y `UserPresenceResponse` suman el campo `ownedBotMatch`; `busy` pasa a true si está presente.

**Rationale**: el usuario eligió un **campo nuevo** separado de `spectating`, para desacoplar "soy
dueño (ocupado)" de "estoy mirando activamente". No obliga a estar mirando.

## D9 — Estadísticas/logros ya excluyen bots (sin cambios)

**Verificado**: `ProfilePlayerStatsTrackingService.handle` corta temprano si
`botRegistry.isBot(playerOne) || botRegistry.isBot(playerTwo)` (líneas 46-49). En bot-vs-bot ambos
son bots → no se registran stats ni outcomes (FR-015) sin cambios. Los logros se evalúan sobre
jugadores registrados; los bots no tienen perfil → no se disparan.

## D10 — Liberación al terminar (sin cambios de cleanup)

**Decisión**: no hace falta un handler nuevo de liberación. La ocupación se deriva del **estado del
match**: cuando llega `MatchFinishedEvent` (o forfeit/abandon/cancel), `findActiveOwnedMatchId` deja
de devolver ese match → el creador queda disponible (FR-012). `SpectatorCleanupOnMatchEnd` ya limpia
cualquier `Spectatorship` activa del creador si estaba mirando.
