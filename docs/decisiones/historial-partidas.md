# Historial de partidas

> Decisión de dominio no obvia. Verificá contra el estado actual del código antes de asumir que
> sigue vigente.

Bounded context `com.villo.truco.history`, **totalmente separado** del resto. Mantiene, por jugador,
el resumen de sus **últimas 5 partidas terminadas** (rival, ganó/perdió, juegos a favor/en contra,
motivo de fin, fecha). Calca el patrón de los **logros/stats de match** (`profile`): se arma
reaccionando a los `MatchDomainEvent` genéricos que el agregado Match **ya emite**, sin que Match lo
conozca ni emita nada a medida del receptor.

**Por qué así (y no como campaña).** El requisito explícito fue evitar el acoplamiento donde el
emisor diseña eventos pensando en quién los consume. El historial **no** agrega campos a los eventos
de Match, **no** usa un registry paralelo y **no** hace pull cross-domain: solo consume los tres
eventos finales que ya existen.

**Agregado `PlayerMatchHistory`** (uno por jugador, key = `PlayerId`): lista de hasta
`MAX_ENTRIES = 5` `MatchHistoryEntry`, ordenada por `endedAt` descendente. `record(entry)` es
**idempotente por `matchId`** (dedup ante doble entrega) y trunca a 5 (descarta la más vieja).
Persistencia JSONB + `@Version` (optimistic locking), igual que `MatchAchievementTracker`.

**Flujo.** `HistoryMatchDomainEventHandler` (registrado en el dispatcher de match de
`EventNotifierConfiguration`, in-transaction) delega en `MatchHistoryTrackingService`, que reacciona
solo a `MatchFinished/Abandoned/Forfeited`. Por cada seat que sea **jugador registrado** arma una
entrada desde su perspectiva (`outcome` según `winnerSeat`, `endReason` según el tipo de evento) y
la persiste. El rival puede ser un bot: se guarda su `PlayerId`; **los bots no obtienen historial
propio** (no son usuarios registrados).

**Lectura.** `GET /api/match-history` (controller en `history.infrastructure.http`) opera sobre el
usuario autenticado vía `GetPlayerMatchHistoryUseCase`. El query handler resuelve el nombre del
rival(username vía `UserQueryRepository`, o `displayName` del bot vía `BotRegistry`) — esa
resolución es responsabilidad de la query, no del dominio del historial.

**Fuera de alcance.** No se guarda el "modo" de la partida (casual/liga/copa/campaña/vs bot) porque
los eventos finales no lo traen y agregarlo implicaría acoplar Match. Tampoco hay push WebSocket: el
historial se consulta por REST. Es distinto del **gameplay recording** (feature 015), que es un log
append-only crudo para entrenar bots.
