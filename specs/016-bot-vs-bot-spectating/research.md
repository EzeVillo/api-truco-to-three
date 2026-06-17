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

  boolean isBotVsBotMatch(
      MatchId matchId);                 // existsById — veto + manos + eligibility

  Optional<PlayerId> findOwnerByMatchId(MatchId matchId);   // eligibility owner-only

  Optional<MatchId> findActiveOwnedMatchId(
      PlayerId ownerId); // ocupación/presencia (join con estado)

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

**Decisión**: agregar `BlockingReason.OWNS_BOT_MATCH` a
`PlayerAvailabilityChecker.findBlockingReason`
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

## D5 — Manos de ambos bots por WebSocket (y cierre de una fuga preexistente)

**Decisión**: las manos de ambos bots se entregan al espectador **por WebSocket, como en un partido
normal** (decisión explícita del usuario), no solo en el snapshot. Dos canales, igual que un
jugador:

1. **Estado inicial** (al conectarse a media partida): `SpectatorRoundStateDTO` suma
   `handPlayerOne`/`handPlayerTwo` (`List<CardDTO>`), poblados **solo** en bot-vs-bot
   (`botVsBotMatchRegistry.isBotVsBotMatch`), vía `match.getCardsOf(playerOne/Two)`. En partidas con
   humanos quedan `null`. Esto entra en `SPECTATE_STATE` y `GET /spectate`.
2. **En vivo**: el espectador recibe los eventos de mano de **ambos** asientos: `HAND_DEALT` (lleva
   las dos manos) en cada reparto y `PLAYER_HAND_UPDATED` (por asiento) cuando una mano cambia al
   jugar una carta. Igual que un jugador normal recibe la suya.

### Hallazgo crítico — fuga actual de cartas a espectadores

El `CompositeEventDispatcher.dispatchEvents` invoca a **todos** los handlers por evento (el `return`
dentro de `MatchNotificationEventTranslator.handle` no corta la cadena). Como `HandDealtEvent`
**no** implementa `SeatTargetedEvent`, el `SpectatorNotificationEventTranslator` (que solo descarta
`SeatTargetedEvent`) **hoy publica `HAND_DEALT` con AMBAS manos a cualquier espectador**, también en
partidas con humanos. Es una fuga de privacidad preexistente y contradice el requisito del usuario
("ver las cartas no aplica a ningún otro caso de spectate").

### Cambio: `SpectatorNotificationEventTranslator` recibe `BotVsBotMatchRegistry`

Nueva regla de filtrado (la consulta al registro se hace **solo** para eventos de mano, no en el
camino caliente de los eventos públicos):

```text
inner = unwrap(event)
if (inner es HandDealtEvent o SeatTargetedEvent) {
  esEventoDeMano = inner es HandDealtEvent o PlayerHandUpdatedEvent
  if (!esEventoDeMano) return;                         // AVAILABLE_ACTIONS_UPDATED: nunca al espectador
  if (!registry.isBotVsBotMatch(matchId)) return;      // manos: SOLO en bot-vs-bot (cierra la fuga)
  // bot-vs-bot: HAND_DEALT (ambas manos) y PLAYER_HAND_UPDATED (por asiento) pasan
}
... fan-out a espectadores con mapper.map(inner)
```

Efecto:

- **Partidas con humanos**: el espectador deja de recibir `HAND_DEALT` (se cierra la fuga) y sigue
  sin recibir `PLAYER_HAND_UPDATED`/`AVAILABLE_ACTIONS_UPDATED`. Sin manos, como debe ser.
- **bot-vs-bot**: el espectador (creador) recibe `HAND_DEALT` con ambas manos y
  `PLAYER_HAND_UPDATED`
  de ambos asientos, en vivo.

`mapHandDealt` ya emite ambos asientos (`{player_one:[...], player_two:[...]}`) y
`mapPlayerHandUpdated` emite `{seat, cards}`; no hace falta tocar el `MatchEventMapper`. La
redacción
hacia el jugador (`MatchNotificationEventTranslator.publishHandDealt`) no se toca.

**Alternativa rechazada**: dejar las manos solo en el snapshot + refetch en `ROUND_STARTED`. El
usuario pidió explícitamente que vengan por WS como un partido normal; además no cerraría la fuga.

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
auto-suscribe al creador como espectador: la ocupación es por autoría; si quiere ver, se suscribe
por
WS (la eligibility owner-only lo habilita). El motor de bots juega solo
(`RoundStartedEvent → BotDomainEventTranslator → BotTurnRequired → AsyncBotActionExecutor`).

## D8 — Presencia: campo nuevo `ownedBotMatch`

**Decisión**: `UserPresenceResolver` suma una resolución `ownedBotMatch` vía
`botVsBotMatchRegistry.findActiveOwnedMatchId(player)` + estado del match
(`MatchQueryRepository`), produciendo `ActiveOwnedBotMatchRefDTO(matchId, status)`.
`UserPresenceDTO`
y `UserPresenceResponse` suman el campo `ownedBotMatch`; `busy` pasa a true si está presente.

**Rationale**: el usuario eligió un **campo nuevo** separado de `spectating`, para desacoplar "soy
dueño (ocupado)" de "estoy mirando activamente". No obliga a estar mirando.

## D9 — Stats y logros en bot-vs-bot (sin cambios, mecanismos distintos)

**Verificado** — y son dos mecanismos diferentes, ojo:

- **Stats** (`ProfilePlayerStatsTrackingService.handle`, líneas 46-49): corta temprano si
  `botRegistry.isBot(playerOne) || botRegistry.isBot(playerTwo)`. O sea, **contra cualquier bot no
  hay stats** (ni en humano-vs-bot ni en bot-vs-bot).
- **Logros** (`ProfileAchievementTrackingService.handle`): **NO** descarta bots de entrada. Evalúa
  el `MatchAchievementTracker` y, recién al desbloquear, filtra por **usuarios registrados**
  (`registeredPlayers = userQueryRepository.findUsernamesByIds(...)`, líneas 71-81): solo desbloquea
  para playerIds que son cuentas de usuario. Por eso, en **humano-vs-bot el humano SÍ puede
  desbloquear logros** (el bot no), tal como espera el producto.

**Consecuencia para bot-vs-bot**: ningún asiento es usuario registrado → la lista de desbloqueos se
filtra a vacío → **no se desbloquea ningún logro** (FR-015 se cumple). El `MatchAchievementTracker`
igual se persiste como bookkeeping interno por match (inocuo, no es algo visible al usuario). **Sin
cambios de código**; FR-015 se sostiene por el filtro de "usuario registrado", no por excluir bots.

## D10 — Liberación al terminar (sin cambios de cleanup)

**Decisión**: no hace falta un handler nuevo de liberación. La ocupación se deriva del **estado del
match**: cuando llega `MatchFinishedEvent` (o forfeit/abandon/cancel), `findActiveOwnedMatchId` deja
de devolver ese match → el creador queda disponible (FR-012). `SpectatorCleanupOnMatchEnd` ya limpia
cualquier `Spectatorship` activa del creador si estaba mirando.
