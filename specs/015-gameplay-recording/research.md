# Research: Recopilación de partidas para entrenamiento de bots

**Feature**: 015-gameplay-recording | **Date**: 2026-06-15

Este documento consolida las decisiones de diseño y las alternativas evaluadas. No quedan
`NEEDS CLARIFICATION` pendientes.

---

## D1 — Estrategia de persistencia: log append-only, NO event sourcing

- **Decisión**: persistir un **log append-only** (`match_action_log`) como stream paralelo, dejando
  intacta la fuente de verdad actual del `Match` (`MatchJpaEntity` + snapshot). NO se convierte el
  agregado a event sourcing.
- **Rationale**: el objetivo es recopilar datos para análisis, no reconstruir el estado por replay.
  Event sourcing exigiría reescribir persistencia y rehidratación del agregado `Match`, con alto
  riesgo y costo de tests, para un beneficio que el log append-only ya entrega. Cumple Principio V
  (YAGNI).
- **Alternativas**:
  - *Event sourcing completo*: rechazado por complejidad/riesgo desproporcionados.
  - *Persistir los `DomainEvent` crudos*: hoy se emiten y se descartan (`clearDomainEvents()`);
    persistirlos daría la acción pero no el estado-de-decisión completo y sin redactar. Se prefiere
    el snapshot (ver D2).

## D2 — Captura del estado: snapshot resultante keyado por `stateVersion`

- **Decisión**: por cada transición se guarda el `MatchSnapshot` **resultante** (post-acción) más la
  acción que la produjo. El par de entrenamiento "estado previo + acción" se deriva al consumir:
  el snapshot en versión `v` es el estado-de-decisión de la acción que llevó a `v+1`.
- **Rationale**: `MatchSnapshot` ya existe (spec 004), es **completo y sin redactar** (incluye ambas
  manos vía `MatchSnapshotExtractor.extractHand(handPlayerOne/Two)`), vive server-side y nunca viaja
  al cliente. `stateVersion` es un contador monótono por partida → cursor sin huecos ni duplicados.
- **Alternativas**:
  - *Serializar a mano una "vista de decisión" nueva*: redundante; `MatchSnapshot` ya lo cubre.
  - *Guardar solo el evento y reconstruir por replay*: más trabajo al consumir y más frágil. El
    usuario pidió explícitamente "snapshot del momento".

## D3 — Punto de captura: decorator sobre los 6 use cases, POR FUERA del pipeline

- **Decisión**: un `GameplayRecordingDecorator` que implementa `UseCase<C extends MatchActionCommand,
  MatchId>` y envuelve a cada uno de los 6 beans de acción **por fuera** del
  `retryTransactionalPipeline`. En la `@Configuration`:
  `recordingDecorator.decorate( retryTransactionalPipeline.wrap(handler) )::handle`.
- **Rationale**:
  - El pipeline (`OptimisticLockRetry` + `Transactional`) envuelve al handler. Colocar el decorator
    **por fuera** garantiza que su lógica de registro corra **después del commit** y en su **propia
    transacción**, satisfaciendo FR-009/FR-010 (no altera ni revierte la jugada).
  - Tanto el humano (vía controller) como el bot (vía `ExecuteBotTurnCommandHandler`, que inyecta los
    mismos beans `PlayCardUseCase`, etc.) pasan por estos 6 beans → ambos se registran sin código
    específico de bot (FR-004).
  - No se decora `ExecuteBotTurnUseCase` (es orquestador: delega en los 6 atómicos; decorarlo daría
    filas redundantes/vacías).
- **Alternativas**:
  - *`PipelineBehavior` global en `retryTransactionalPipeline`*: aplicaría a TODOS los use cases
    (Create/Start/Abandon/Leave), sobre-capturando. Rechazado por scope.
  - *`PipelineBehavior` en un pipeline dedicado `[Retry, Recording, Transactional]`*: viable e
    idiomático, pero el decorator surgical sobre los 6 beans es más explícito y acotado. Rechazado
    por simplicidad de lectura; se documenta como opción válida si en el futuro se prefiere.
  - *Una línea `recorder.record(match)` dentro de cada handler*: invade 6 handlers y correría
    dentro de la transacción de la jugada (no post-commit). Rechazado.

## D4 — Aislamiento transaccional y tolerancia a fallos

- **Decisión**: el adapter `JpaGameplayRecorderAdapter.record(...)` es `@Transactional` (abre una tx
  nueva, ya que post-commit no hay tx activa). El decorator envuelve la llamada en try/catch: ante
  excepción, **loguea y continúa** (no propaga).
- **Rationale**: FR-010 — perder ocasionalmente una entrada es preferible a degradar o revertir la
  partida. La jugada ya commiteó antes de intentar registrar.

## D5 — Derivación de `actorType` y `actorSeat`

- **Decisión**: `actorType` = `BotRegistry.isBot(command.playerId())` → `BOT` / `HUMAN`. `actorSeat`
  = comparar `command.playerId()` con `snapshot.playerOne()/playerTwo()` → `PLAYER_ONE`/`PLAYER_TWO`.
- **Rationale**: `BotRegistry.isBot(PlayerId)` ya existe (puerto de application). El snapshot expone
  ambos `PlayerId`. No hace falta nuevo estado ni columnas en el match.

## D6 — Representación de la acción

- **Decisión**: VO de dominio `RecordedAction` = `RecordedActionType` (enum: `PLAY_CARD`,
  `CALL_TRUCO`, `RESPOND_TRUCO`, `CALL_ENVIDO`, `RESPOND_ENVIDO`, `FOLD`) + un detalle mínimo
  (la carta jugada, el canto o la respuesta, según el tipo). Un `RecordedActionFactory` (application)
  mapea cada uno de los 6 commands → `RecordedAction`. La infra serializa el detalle a JSONB.
- **Rationale**: aunque la acción es parcialmente recuperable del snapshot resultante (la carta
  aparece en `currentRound.currentHandCards`, los cantos en el truco/envido state), guardar
  `action_type` como columna + `action_detail` JSONB hace el dataset trivialmente consultable por SQL
  (FR-005) sin parsear el snapshot.
- **Alternativas**: *solo `action_type` + derivar detalle del snapshot*: más barato pero menos
  cómodo de consultar. Rechazado: el detalle es chico y el costo marginal.

## D7 — Idempotencia, orden e inmutabilidad

- **Decisión**: PK propia + `UNIQUE(match_id, state_version)`; inserción `ON CONFLICT DO NOTHING`.
  Append-only por convención: el adapter sólo hace `INSERT` (nunca UPDATE/DELETE).
- **Rationale**: `state_version` monótono garantiza orden sin huecos (FR-006). El UNIQUE evita
  duplicados ante un eventual reintento (FR-013). No se agrega trigger de inmutabilidad a nivel DB
  (YAGNI); se respeta por diseño del adapter (FR-007).

## D8 — Serialización JSONB

- **Decisión**: reutilizar el mecanismo de serialización JSON que `MatchMapper`/`MatchJpaEntity` ya
  emplean para `current_round`. Columnas `match_state` (snapshot completo) y `action_detail` como
  JSONB.
- **Rationale**: consistencia con la persistencia existente; sin dependencias nuevas.

## D9 — `schema_version`

- **Decisión**: columna `schema_version INT NOT NULL`, constante de aplicación inicial = `1`.
- **Rationale**: FR-008 — permite reinterpretar registros viejos si cambian reglas/forma del
  snapshot. Se incrementa manualmente cuando cambie el formato capturado.

## D10 — Alcance de exportación (recorte del usuario)

- **Decisión**: NO se construye endpoint, comando CLI ni serializador de export. El acceso es por
  consulta SQL directa a `match_action_log`.
- **Rationale**: el usuario es admin y consultará la base directamente; un export no aporta valor
  ahora (YAGNI). La US2 de la spec se satisface con datos consultables.
