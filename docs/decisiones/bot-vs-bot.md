# Bot-vs-bot: avance manual y matches sin timeout

> Decisiones de dominio no obvias. Verificá contra el estado actual del código antes de asumir que
> siguen vigentes.

## Las partidas bot-vs-bot no avanzan solas

Cada acción del bot se ejecuta a pedido del dueño vía
`POST /api/matches/bot-vs-bot/{matchId}/advance` (owner-only, 204, idempotente, **una** acción por
request). Jugador-vs-bot queda **igual** (el bot sigue respondiendo solo).

**Por qué (decisión de diseño):** el "avance automático" no es concepto de dominio, es orquestación
de application, y ya vivía ahí (binomio `BotDomainEventTranslator` → `AsyncBotActionExecutor`). El
dominio `match` sigue neutral (no sabe de bots); el dominio `bot` es de *decisión* (qué jugada), no
de *orquestación* (cuándo se dispara). Meter `autoAdvance` en `match` se descartó porque inferiría
que hay bots.

**Cómo se implementó:**

- `BotDomainEventTranslator` consulta `BotVsBotMatchRegistry.isBotVsBotMatch(matchId)` y **no
  publica `BotTurnRequired`** si lo es → corta la cadena automática (arranque y turnos siguientes).
- `AdvanceBotVsBotMatchCommandHandler` valida owner vía `findOwnerByMatchId`, toma lock pesimista
  (`findByIdForUpdate`, serializa advance/abandon), resuelve qué bot tiene
  `getDecisionViewFor(p).hasAvailableActions()` y delega a `ExecuteBotTurnUseCase`.
- El front sabe de quién es el turno por `currentRound.currentTurn` del estado de espectado (ya
  existía); recibe el nuevo estado por el push de espectado tras cada avance.

## Habilitador: matches vs bots sin timeout por inactividad

Los matches vs bots **no forfeitean por inactividad**. La regla vive como
`MatchRules.forfeitsOnInactivity` (no existe el concepto "bot" en el dominio): se traduce a `false`
en `CreateBotMatch` y en `RematchConfirmed`, y se filtra en la projection de timeouts.

Esto es lo que habilita bot-vs-bot: un match pausado esperando el `advance` **no muere por timeout
**.
