# Research: Presencia en tiempo real (push de ocupación)

**Feature**: `009-presence-push` | **Fase**: 0 (Outline & Research)

Resolución de las decisiones técnicas abiertas antes del diseño. Cada punto sigue el formato
Decisión / Rationale / Alternativas.

## 1. Forma del payload: snapshot completo vs. delta por dominio

**Decisión**: Cada notificación lleva el **snapshot completo** de presencia del usuario (los mismos
campos que `UserPresenceResponse` de la 008: `busy` + `match` + `league` + `cup` + `rematch`), más
metadatos livianos (`eventType`/dominio que originó el cambio y `timestamp`) para UX del FE.

**Rationale**:

- **Coherencia entre dominios (FR-013)**: entrar a la partida de un torneo cambia `match` **y**
  `league.currentMatchId` a la vez; un único snapshot los refleja consistentes, sin condiciones de
  carrera entre dos deltas separados.
- **Idempotencia (FR-002)**: la sesión originante recibe el mismo snapshot que ya refleja su estado;
  es un no-op natural. Reentregas o reordenamientos no rompen nada (el último snapshot gana).
- **Reconciliación (FR-011)**: el snapshot es byte-a-byte equivalente a un pull de la 008, por lo
  que
  el FE usa un único modelo de datos para ambos caminos (pull inicial + push de cambios).

**Alternativas consideradas**:

- *Delta por dominio* (`{domain: MATCH, ref: {...}, op: ENTERED|RELEASED}`): menor payload, pero
  exige
  al FE mantener una máquina de estados y resolver coherencia multi-dominio del lado cliente; más
  superficie de bug. Rechazado por YAGNI y por el costo de coherencia.

## 2. Reutilizar la resolución de la 008 (`UserPresenceResolver`)

**Decisión**: Extraer la lógica de resolución de `GetUserPresenceQueryHandler` a un
`UserPresenceResolver` (application) que dado un `PlayerId` devuelve un `UserPresenceDTO`. El
handler
de la 008 delega en él (sin cambio de comportamiento). Los traductores de presencia lo invocan para
construir el snapshot de cada jugador afectado.

**Rationale**: Una sola fuente de verdad para el shape de presencia → push y pull nunca divergen.
Evita duplicar la lógica de refs (la regla `currentMatchId` solo-si-`IN_PROGRESS`, el orden
`findInProgress` → `findWaiting`, etc.). Respeta DRY y Simplicidad (Principio V).

**Alternativas consideradas**:

- *Construir el ref desde el payload del evento*: evita re-consultar, pero los eventos no cargan
  todo
  lo necesario (p. ej. `PlayerJoinedEvent` no trae el `status` de la partida) y obligaría a duplicar
  y
  mantener en paralelo la lógica de la 008. Rechazado.

## 3. Visibilidad transaccional de la re-resolución

**Decisión**: Re-resolver la presencia **durante el despacho de domain events**, que ocurre
**después** de persistir el agregado y dentro de la misma transacción del command handler.

**Rationale**: El patrón confirmado en los handlers es `repository.save(aggregate)` →
`eventNotifier.publishDomainEvents(...)` (ver
[CreateMatchCommandHandler](../../src/main/java/com/villo/truco/application/usecases/commands/CreateMatchCommandHandler.java)).
Como la re-resolución corre dentro de esa misma transacción/persistence-context tras el `save`, las
consultas (`MatchQueryRepository`, etc.) observan el estado **post-transición** (p. ej. tras
`MatchFinishedEvent`, la partida ya figura finalizada → `findUnfinishedByPlayer` vacío → snapshot
"liberado"). Es exactamente la garantía de la que ya dependen los notificadores STOMP existentes
(social, match), que también empujan durante el despacho.

**Riesgo asumido / mitigación**: si la transacción hace rollback **después** del push, la
notificación queda "fantasma" (mismo riesgo que ya tienen los notificadores actuales del proyecto).
Se acepta por consistencia con el patrón vigente; además el FE reconcilia con la 008 (best-effort,
FR-011). No se introduce manejo transaccional nuevo (YAGNI).

## 4. Eventos de dominio que disparan una notificación de presencia

**Decisión**: Notificar solo en **transiciones de ocupación** (entrar, avanzar de partida de torneo,
abrir/cerrar revancha, liberar), no en cambios de estado intra-partida. Eventos por dominio:

| Dominio  | Entrar / actualizar ocupación                                                                                                                                                                                                                                            | Liberar ocupación                                                                                                 |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Partida  | `PlayerJoinedEvent` (alta del 2º jugador) + `GameStartedEvent` con `gameNumber == 1` (único borde en flujos sin "join": bot match y quick match, donde el creador nunca dispara PlayerJoined; las partidas de torneo las cubre el traductor de liga/copa al re-resolver) | `MatchFinishedEvent`, `MatchCancelledEvent`, `MatchAbandonedEvent`, `MatchForfeitedEvent`, `MatchPlayerLeftEvent` |
| Liga     | `LeaguePlayerJoinedEvent`, `LeagueStartedEvent`, `LeagueMatchActivatedEvent`, `LeagueAdvancedEvent`                                                                                                                                                                      | `LeagueFinishedEvent`, `LeagueCancelledEvent`, `LeaguePlayerLeftEvent`, `LeaguePlayerForfeitedEvent`              |
| Copa     | `CupPlayerJoinedEvent`, `CupStartedEvent`, `CupMatchActivatedEvent`, `CupAdvancedEvent` (y `CupBoutActivatedEvent` si aplica)                                                                                                                                            | `CupFinishedEvent`, `CupCancelledEvent`, `CupPlayerLeftEvent`, `CupPlayerForfeitedEvent`                          |
| Revancha | `RematchSessionOpenedEvent`                                                                                                                                                                                                                                              | `RematchSessionConfirmedEvent`, `RematchSessionClosedByLeaveEvent`, `RematchSessionExpiredEvent`                  |

**Rationale**:

- Los cambios de status **dentro** de una partida ya activa (READY → IN_PROGRESS, turnos, etc.) no
  son
  transiciones de ocupación: el usuario ya fue derivado y está sobre `/user/queue/match`.
  Notificarlos
  sería ruido (Principio V). La presencia cubre **bordes** de ocupación.
- Como el payload es un snapshot re-resuelto, alcanza con suscribir el conjunto de eventos que
  **cruzan** un borde de ocupación; la lista exacta se valida en implementación contra los emisores
  reales de cada agregado.

**Alternativas consideradas**:

- *Reaccionar a todos los domain events*: mucho ruido y re-resoluciones inútiles. Rechazado.

## 5. Jugadores afectados y exclusión de bots

**Decisión**: De cada evento se extraen los jugadores afectados (partida/revancha:
`playerOne`/`playerTwo`; liga/copa: `participants` o el `playerId` que el evento exponga) y se
**filtran los bots** vía `BotRegistry`. Se emite **una** `PresenceEventNotification` por **cada
jugador humano**, con su propio snapshot y como único destinatario.

**Rationale**: El snapshot es específico de cada usuario (su ocupación), así que no se puede
compartir
un payload entre destinatarios (a diferencia de las notificaciones sociales). Los bots no tienen
sesiones; filtrarlos evita pushes inútiles. `BotRegistry` ya se usa con este fin en otros
traductores.

**Alternativas consideradas**: enviar a todos los participantes sin filtrar bots → pushes a destinos
inexistentes; descartado.

## 6. Canal de transporte y nombre de la cola

**Decisión**: Reutilizar la infraestructura STOMP existente y empujar a **`/user/queue/presence`**
vía
`SimpMessagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(playerId), "/queue/presence", wsEvent)`.

**Rationale**: Es el mismo mecanismo de `/user/queue/social`. Las colas de usuario STOMP son
dinámicas: no requieren registro adicional en `WebSocketConfig` más allá del prefijo `/user` ya
configurado. Una cola dedicada permite al FE suscribirse solo a presencia. La autenticación y el
`Principal` (FR-009) los provee el `WebSocketAuthInterceptor` ya existente.

**Alternativas consideradas**: reutilizar `/user/queue/social` → mezcla dominios y complica el FE;
descartado por claridad de contrato.

## 7. Observabilidad

**Decisión**: Registrar éxito/fallo del push en el `EventNotifierHealthRegistry` existente (igual
que
`StompSocialNotificationHandler`).

**Rationale**: Consistencia con la observabilidad ya presente; sin métricas nuevas (YAGNI).

## Resumen de decisiones

| # | Tema                      | Decisión                                                              |
|---|---------------------------|-----------------------------------------------------------------------|
| 1 | Payload                   | Snapshot completo (forma de `UserPresenceResponse`) + metadatos       |
| 2 | Resolución                | `UserPresenceResolver` extraído de la 008, reutilizado por el push    |
| 3 | Visibilidad transaccional | Re-resolver tras `save`, dentro de la tx; best-effort + pull de 008   |
| 4 | Disparadores              | Solo transiciones de ocupación (entrar/avanzar/abrir/liberar)         |
| 5 | Destinatarios             | Una notificación por jugador humano; bots filtrados con `BotRegistry` |
| 6 | Transporte                | STOMP `/user/queue/presence` (infra existente)                        |
| 7 | Observabilidad            | `EventNotifierHealthRegistry` (reutilizado)                           |
