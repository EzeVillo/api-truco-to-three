# Rematch y ocupación de jugadores

> Decisiones de dominio no obvias. Verificá contra el estado actual del código antes de asumir que
> siguen vigentes.

## Rematch: solo para victorias normales en matches casuales

Solo `MatchFinishedEvent` dispara creación de sesión de rematch. Las causas alternativas de
terminación NO generan sesión:

| Motivo de finalización        | Evento                | Rematch | Notas                                                         |
|-------------------------------|-----------------------|---------|---------------------------------------------------------------|
| Victoria normal               | `MatchFinishedEvent`  | ✅ SÍ    | Siempre se crea si es casual                                  |
| Abandono (player left)        | `MatchAbandonedEvent` | ❌ NO    | No hay `*RematchSessionCreator` para este evento              |
| Timeout (idle inactivity)     | `MatchForfeitedEvent` | ❌ NO    | `TimeoutIdleMatchesCommandHandler` → `match.timeoutForfeit()` |
| Cancelación (waiting timeout) | `MatchCancelledEvent` | ❌ NO    | En estado WAITING_FOR_PLAYERS si vence timeout                |

**Condiciones para crear sesión de rematch:**

1. Match es **casual** (no está en liga/copa ni campaña).
2. Match terminó normalmente (= `MatchFinishedEvent`).
3. Ambos jugadores identificables (`playerTwo != null`).

Los matches de **liga/copa** y **campaña** quedan vetados por implementaciones de `RematchVeto`
(`RematchEligibilityPolicy` recibe `List<RematchVeto>`). TTL configurable:
`truco.rematch.duration=PT2M`.

**Por qué:** decisión de UX — el rematch no tiene sentido para flujos de error (abandono, timeout
del
sistema). Mantiene la feature simple: "revancha para partidas completas y justas".

**Cómo aplicar:** para permitir rematch en abandono/timeout habría que agregar handlers de rematch
(`*RematchSessionCreator`) para `MatchAbandonedEvent` y `MatchForfeitedEvent`.

## Detección de "jugador entra a una partida" (eventos de ocupación)

Para detectar que un jugador humano pasa a estar ocupado en una partida (p. ej. el push de presencia
de la feature 009), no alcanza con `PlayerJoinedEvent`:

- `PlayerJoinedEvent` solo se emite cuando un **segundo** jugador se une a un match existente
  (`Match.joinPrivate`/`joinPublic`). El **creador** nunca lo dispara.
- **Bot match** (`CreateBotMatchCommandHandler`) y **quick match** (`Match.quickMatch`) usan
  `createReady`/constructor + `startInternal` y emiten `GameStartedEvent`, **nunca**
  `PlayerJoinedEvent`.
- `GameStartedEvent` se emite **por cada juego** (`gameNumber` incrementa). El primer juego
  (`gameNumber == 1`) marca el inicio real del match (entra en `IN_PROGRESS` por primera vez).

**Regla:** "entró a un match" = `PlayerJoinedEvent` **OR** `GameStartedEvent` con `gameNumber == 1`.
Las partidas de torneo (liga/copa) se cubren aparte vía los eventos `*MatchActivated`/`*Started` de
esos dominios.

**El que CREA también queda ocupado al instante** (`PlayerAvailabilityChecker.ensureAvailable`
rechaza
si tenés un match no finalizado o una liga/copa en espera). Por eso hay eventos de dominio dedicados
emitidos en la creación: `MatchCreatedEvent` (en `Match.create`), `LeagueCreatedEvent`,
`CupCreatedEvent` — todos vía `addDomainEvent` (no transicional, fuera del stateVersion stream). Sin
ellos, el host que crea y espera solo no recibía push (el GET sí lo veía). Ojo: agregar un evento a
un `create` rompe tests que afirman la cantidad exacta de eventos publicados
(`CreateMatch/League/CupCommandHandlerTest`).
