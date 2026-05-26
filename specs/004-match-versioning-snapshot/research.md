# Research — Versionado de Match para Reconciliación Snapshot + Stream

Fecha: 2026-05-25
Branch: `004-match-versioning-snapshot`

Este documento resuelve los NEEDS CLARIFICATION del plan y registra decisiones técnicas con su
justificación y alternativas evaluadas.

---

## D-01 — Dónde vive el contador de transiciones

**Decisión**: en el agregado `Match` (campo `stateVersion: long`), persistido junto al resto del
estado en `matches.state_version`. NO reutilizar el `@Version` JPA existente.

**Rationale**:

- El `@Version` actual de Hibernate se incrementa una vez por `save`, no por evento. Un comando
  (p.ej. `playCard`) puede emitir N eventos transicionales en la misma transacción y todos
  comparten el mismo `@Version`. No cumple FR-001 ("incrementa en exactamente uno por cada
  transición").
- Mezclar dos semánticas sobre el mismo campo (optimistic locking + cursor del cliente) genera
  acoplamiento frágil: cualquier `save` adicional (p. ej. para actualizar metadata) rompe la
  monotonía del cursor cliente.

**Alternativas consideradas**:

- *Reutilizar `@Version`*: descartada por lo anterior.
- *Persistir como event store separado*: descartada (overkill, viola YAGNI; el alcance es
  snapshot+stream, no event sourcing).

---

## D-02 — Tipo del contador

**Decisión**: `long` (64 bits, sin signo lógico, parte desde 0).

**Rationale**: un `int` (2³¹) podría llegar a ser estrecho ante reusos del id o futuras
extensiones, y `long` es coste cero. Default `0` para matches reconstruidos antes de la
migración (ningún evento aún emitido en la sesión actual).

**Alternativas**: `int`. Descartada por margen.

---

## D-03 — Cuándo se incrementa

**Decisión**: cada vez que `Match` añade un evento **transicional** a su lista interna, el
contador se incrementa **antes** de etiquetar el evento, y el evento queda etiquetado con el
valor resultante (el "número de transición tras aplicar el evento", según FR-003). Esto se hace
en una función única dentro del aggregate (`addTransitionalEvent(...)`) que reemplaza llamadas
directas a `domainEvents.add(...)` para eventos transicionales.

**Casos compuestos**:

- Un `MatchEventEnvelope` (que envuelve un evento producido por `Round`) cuenta como **una sola
  transición**: el envelope recibe un `stateVersion` y delega los getters al inner. Cada inner
  distinto = un envelope distinto = un increment distinto.

**Rationale**: alinea con la regla "todo evento de transición tiene su número", evita doble
conteo y mantiene la fuente de la verdad dentro del aggregate (no en application/infra).

**Alternativas**:

- *Asignar en application al publicar*: descartada porque dispersa la regla y dificulta tests de
  dominio puro.
- *Incrementar al persistir (`@PreUpdate`)*: descartada por las mismas razones que D-01.

---

## D-04 — Qué eventos son transicionales vs derivados

**Decisión**: lista cerrada y explícita. Marcador en código vía interface
`MatchTransitionalEvent` (marker) implementado por los eventos que cuentan; o, alternativa
equivalente, marker `MatchDerivedEvent` para los que NO cuentan (más corta porque la mayoría son
transicionales).

**Transicionales** (avanzan `stateVersion`, se entregan a **todos** los jugadores, vía
`/user/queue/match`):

- `PlayerJoinedEvent`
- `PlayerReadyEvent`
- `GameStartedEvent`
- `RoundStartedEvent`
- `CardPlayedEvent`
- `TurnChangedEvent` — **decisión**: SÍ es transicional (cambia el turno = cambio del aggregate
  observable por todos).
- `TrucoCalledEvent`, `TrucoRespondedEvent`, `TrucoCancelledByEnvidoEvent`
- `EnvidoCalledEvent`, `EnvidoResolvedEvent`
- `FoldedEvent`
- `HandResolvedEvent`, `RoundEndedEvent`
- `ScoreChangedEvent`, `GameScoreChangedEvent`
- `MatchFinishedEvent`, `MatchAbandonedEvent`, `MatchForfeitedEvent`,
  `MatchCancelledEvent`, `MatchPlayerLeftEvent`
- **Reparto de cartas** (transición lógica nueva, ver D-06): genera un evento de tipo
  `HandDealtEvent` (nombre tentativo) que avanza `stateVersion` UNA vez y se materializa como
  payloads-por-jugador.

**Derivados** (NO avanzan `stateVersion`, son por destinatario, vía
`/user/queue/match-derived`):

- `PlayerHandUpdatedEvent` — hoy emitido por mano del jugador; pasa a ser una proyección
  derivada del estado, no transicional. (En su lugar, `HandDealtEvent` cubre la transición
  para la secuencia global.)
- `AvailableActionsUpdatedEvent` — es una proyección del estado para un jugador; no es una
  nueva transición.

**Excluidos del flujo de stream del match** (canales propios):

- `PublicMatchLobbyOpenedEvent` — ya hoy se traduce a `/topic/public-match-lobby`. Sigue igual,
  no transicional respecto del match.

**Rationale**: la regla de oro de la Estrategia A es: si un evento cambia el estado del aggregate
observable por todos los jugadores, todos lo reciben (aunque con payload redactado) y consume un
`stateVersion`. Si es una derivación por jugador del estado, no consume `stateVersion` y va por
otro canal. Categorías explícitas hacen la regla auditable y testeable; un marker interface
permite enforcement por ArchUnit si se quiere endurecer luego.

**Alternativas**:

- *Inferir por nombre*: frágil.
- *Por capacidad de redactar*: ambiguo (algunos transicionales no tienen info oculta).

---

## D-05 — Canal de entrega para derivados

**Decisión**: nuevo destino STOMP **`/user/queue/match-derived`**.

- Mismo shape de envelope que `MatchWsEvent` pero con `stateVersion = null` (o ausente).
- El cliente sabe que estos eventos NO sirven para detectar huecos.

**Rationale**: tener canales distintos elimina el riesgo de que un cliente cuente derivados
contra la secuencia y vea "huecos" inexistentes (FR-006). El nombre `match-derived` deja claro
que es "derivado del estado del match", no una nueva transición.

**Alternativas**:

- *Mismo destino con flag `transitional: true/false`*: descartada (más fácil de equivocarse al
  parsear; menos auditable en logs/inspectores STOMP).
- *Destinos distintos por tipo (hand, actions)*: overkill.

---

## D-06 — Eventos con información oculta (FR-005, FR-011)

**Decisión**: el reparto de cartas se modela como **un único evento de dominio transicional**
`HandDealtEvent(seatToCards: Map<PlayerSeat, List<Card>>)`. En la capa de publicación,
`MatchNotificationEventTranslator` materializa **dos `MatchEventNotification`** (una por
destinatario) con el **mismo `stateVersion`** y el **mismo `eventType`**, pero con `cards`
filtradas a la mano del destinatario. No se incluyen metadatos derivables (no se expone
`cardsCountOpponent`, ni se incluye un campo `opponentCards: null` que delate presencia: el
payload contiene **únicamente** la lista de cartas propias). Si el espectador llegara a recibir
este evento, recibe `cards: []` (o se le excluye, ver D-09).

**Rationale**: garantiza FR-005 (mismo `stateVersion` y `eventType`) y FR-011 (no filtrar
indirectamente: ni campos vacíos, ni longitudes del oponente, ni hashes). Mantiene un único
evento de dominio (DDD limpio); la responsabilidad de redactar es de la capa de aplicación.

**Alternativas**:

- *Dos `PlayerHandUpdatedEvent` (uno por jugador) como hoy*: rompe FR-005 (eventos distintos,
  distinto `stateVersion` si se incrementara por cada uno). Mantenerlos como derivados ya no
  es suficiente porque el reparto SÍ es una transición global.
- *Reusar `PlayerHandUpdatedEvent` como transicional con dos eventos*: igual problema.

**Consecuencia**: hay que introducir `HandDealtEvent` y dejar de emitir
`PlayerHandUpdatedEvent` como mecanismo de reparto. Si se quiere conservar
`PlayerHandUpdatedEvent` para refrescos puntuales de mano (caso reconexión, recálculo), se
mantiene como **derivado** por `/user/queue/match-derived`.

---

## D-07 — Persistencia y migración

**Decisión**:

- Migración Flyway `VXXX__add_match_state_version.sql`:
  ```sql
  ALTER TABLE matches ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0;
  ```
- `MatchJpaEntity`: agregar
  `@Column(name = "state_version", nullable = false) private long stateVersion;`
- `MatchMapper`: propagar en ambas direcciones (`toEntity`, `toDomain`).
- `Match.reconstruct(...)`: aceptar `stateVersion` como parámetro adicional.
- Default `0` para matches preexistentes; las próximas transiciones incrementarán desde ahí.

**Rationale**: ALTER aditivo con default, sin reescribir filas, sin downtime. Matches viejos
seguirán funcionando: la única invariante que importa es monotonía dentro del mismo match,
garantizada porque las transiciones futuras parten de 0 y suman.

---

## D-08 — Backward compatibility del contrato

**Decisión**: cambios **aditivos**:

- `MatchStateResponse` gana `stateVersion`. Clientes existentes que no lo lean siguen
  funcionando.
- `MatchWsEvent` gana `stateVersion`. Mismo criterio.
- Nuevo destino STOMP `/user/queue/match-derived`: los clientes que no se suscriban no reciben
  nada nuevo; los flujos actuales NO se redirigen sin aviso. Si un cliente todavía espera
  `PLAYER_HAND_UPDATED` por `/user/queue/match`, debe migrar al nuevo destino. **Documentar
  este cambio en `docs/CONTRATOS_API.md`** y comunicarlo al frontend.

**Rationale**: la única no-aditiva es la reclasificación de `PLAYER_HAND_UPDATED` /
`AVAILABLE_ACTIONS_UPDATED` al canal derivado. Es necesario por FR-006; se mitiga con
documentación explícita y se coordina con el frontend antes del merge.

---

## D-09 — Espectadores

**Decisión**: `GET /api/matches/{matchId}/spectate` también devuelve `stateVersion` en el
snapshot. El stream del espectador (`/user/queue/match-spectate`) recibe los eventos
transicionales con su `stateVersion`, con cartas siempre redactadas (vacías o ausentes,
consistente con el modelo actual). Los espectadores **no** reciben el canal derivado de jugador
(no hay derivados propios del espectador en alcance de este feature).

**Rationale**: los espectadores también necesitan reconciliación snapshot+stream; misma regla
sin sobrecargar el feature.

---

## D-10 — Notificaciones de chat y otros bounded contexts

**Decisión**: fuera de alcance. El chat de match, liga, copa, social, profile mantiene sus
canales actuales. Adoptar versionado en esos contextos será un feature futuro independiente.

**Rationale**: principio V (YAGNI) + alcance explícito del spec.

---

## Resumen de impacto en código

- **Dominio**: `Match`, `MatchSnapshot`, `MatchSnapshotExtractor`, `MatchDomainEvent`,
  `MatchEventEnvelope`, nuevo `HandDealtEvent`, marker (`MatchDerivedEvent` o
  `MatchTransitionalEvent`), reclasificación de `PlayerHandUpdatedEvent` y
  `AvailableActionsUpdatedEvent` como derivados.
- **Aplicación**: `MatchStateDTO`, `MatchStateDTOAssembler`, `MatchEventNotification` (+
  `stateVersion`), `MatchNotificationEventTranslator` (separa transicional vs derivado y
  materializa payloads por jugador para eventos con info oculta).
- **Infraestructura**: `MatchJpaEntity` (+ columna), `MatchMapper`, `MatchStateResponse`,
  `SpectatorMatchStateResponse`, `MatchWsEvent` (+ campo nullable), `StompMatchNotificationHandler`
  (selecciona destino según presencia/ausencia de `stateVersion`).
- **Migración Flyway** nueva.
- **Tests**: dominio (invariantes del contador), aplicación (redacción por jugador con mismo
  stateVersion, separación de canales), integración REST y WS.

---

## Cuestiones cerradas

Ningún `NEEDS CLARIFICATION` queda pendiente.
