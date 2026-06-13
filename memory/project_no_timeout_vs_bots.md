---
name: project-no-timeout-vs-bots
description: los matches contra bots no forfeitean por inactividad; la regla vive como forfeitsOnInactivity en MatchRules, no como concepto "bot" en el dominio
metadata:
  type: project
---

Los matches contra bots (bot-match directo, campaign y rematch de bot-match) NO hacen forfeit por
inactividad: el jugador humano puede tardar lo que quiera porque no hay rival humano perjudicado.

**Modelado (cuidado con la capa):** el dominio NO conoce el concepto "bot". La regla se modela como
`MatchRules.forfeitsOnInactivity` (boolean) — una regla del match, agnóstica de bots.
`Match.timeoutForfeit()` arranca con `if (!rules.forfeitsOnInactivity()) return false;` (invariante
dura).

**Traducción isBot → regla** (en application, único punto donde `BotRegistry` toca el modelo): los
DOS sitios que crean un match a partir de participantes-bot la setean en `false`:

- `CreateBotMatchCommandHandler` (cubre bot directo Y campaign, que delega en
  `CreateBotMatchUseCase`).
- `RematchSessionConfirmedMatchCreator` (tiene `BotRegistry` inyectado; computa
  `!isBot(p1) && !isBot(p2)`).

Todos los demás creadores (cup/league/quick/createMatch) pasan `forfeitsOnInactivity = true`
explícito. No hay constructor de conveniencia con default —
ver [[feedback-no-convenience-overloads]].

**OJO — son TRES facetas de la misma regla, no solo el forfeit.** Apagarlas todas para
`forfeitsOnInactivity=false`:

1. **Agendado dirigido por eventos** (`MatchTimeoutEventHandler`): es un camino separado del de
   reconciliación. Lee `forfeitsOnInactivity` estampado en el evento; si es false, `cancelTimeout` y
   no agenda. El flag se estampa en `MatchDomainEvent` dentro de `Match.getMatchDomainEvents()`,
   igual que el `matchStatus` (ver [[project_timeout_phase_from_status]]).
2. **Cuenta regresiva al front** (`ActionDeadlineSetEvent`): si `!rules.forfeitsOnInactivity()`,
   `Match.refreshActionDeadline()` retorna sin emitir NINGÚN evento de deadline (ni Set ni
   Cleared) — el flag es inmutable, así que un bot match nunca tuvo deadline y no hay nada que
   limpiar. Emitir `Cleared` en cada turno sería ruido de WS al pedo. Sin esto el front muestra un
   timer de 30s fantasma.
3. **`actionDeadline` del GET state** (`ActionDeadlineProjection.of`): devuelve los 3 campos en null
   si `!match.forfeitsOnInactivity()` (método público nuevo en `Match`).

**Optimización (infra):** la projection
`SpringDataMatchRepository.findActiveMatchesWithLastActivity` filtra
`AND m.forfeitsOnInactivity = true`, igual que ya filtraba por status, para que los bot-matches ni
entren al camino de reconciliación (cubre safety-net). Columna `forfeits_on_inactivity` (V20,
DEFAULT TRUE).

Síntoma si se olvida alguna faceta: en los logs igual aparece "Timeout programado ... etaSeconds=30"
y "Cannot process idle match, skipping" (el guard del dominio salva el forfeit, pero se agenda al
pedo y el front ve el countdown).

Nota tests: en ITs con insert crudo de `matches` (Hibernate create-drop, sin default) hay que
incluir la columna `forfeits_on_inactivity`.
