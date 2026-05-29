---

description: "Lista de tareas — Deadline de turno como concepto de dominio"
---

# Tasks: Deadline de turno como concepto de dominio

**Input**: Documentos de diseño en `/specs/005-turn-deadline-domain/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/deadline-contract.md

**Tests**: INCLUIDOS. El Principio III de la constitution exige tests con títulos en español y
cobertura mínima del 70%.

## Estado de implementación (2026-05-28)

> ⚠️ **El entorno no pudo ejecutar Gradle** (`Unable to establish loopback connection`), así que el
> código fue escrito pero **no se compiló ni se corrieron los tests aquí**. Falta correr
> `./gradlew build` localmente (T037).

**Desviación de diseño respecto del plan (D4):** se decidió **no inyectar `now` en el dominio** para
evitar cambiar ~10 firmas de métodos de `Match` y el churn masivo en `MatchTest`. En su lugar:

- El dominio **decide quién debe actuar y cuándo corre el reloj**: `Match.refreshActionDeadline()`
  emite `ActionDeadlineSetEvent(seat)` / `ActionDeadlineClearedEvent` como eventos derivados al
  final
  de cada acción (núcleo DDD intacto).
- El **ancla temporal** sigue siendo `lastActivityAt` (estampada por infra en `@PreUpdate`, sin
  cambio → T007 NO aplicado).
- La **proyección**: el snapshot usa `lastActivityAt + idleTimeout`; el evento en vivo usa
  `event.timestamp + idleTimeout` (mapper). Ambos representan "momento de la última acción +
  duración" y coinciden dentro de milisegundos → **dentro de la tolerancia de SC-001 (±1s)**.
- Consecuencia: T004/T007/T009/T010 (threading de `now` + quitar `@PreUpdate`) **no se aplicaron**;
  T032 se reduce a verificación (el ancla ya se persiste). Si se quisiera el anclado exacto al
  milisegundo (un único instante idéntico en las 3 vías), habría que hacer el threading de `now`.

**Tareas hechas (código):** T002, T003, T005, T006(verif), T011, T012(verif), T013, T014, T017,
T018, T019, T020, T024, T025, T026, T027(verif), T031(verif), T032(verif), T033, T034.
Además: nuevo helper `ActionDeadlineProjection` (fuente única de la fórmula) y fix en
`MatchTimeoutEventHandler` para ignorar los eventos de deadline.

**Pendientes (requieren build corriendo):** T001, T015, T016, T022, T028, T029, T030 (tests de
integración/assembler), T035 (README), T036 (quickstart), T037 (`./gradlew build`). T023 queda
cubierto genéricamente por `SpectatorNotificationEventTranslatorTest` existente (los eventos no son
`SeatTargetedEvent`).

## Format: `[ID] [P?] [Story] Descripción`

- **[P]**: Puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: A qué user story pertenece (US1, US2, US3)

## Convención de rutas

Proyecto único hexagonal: `src/main/java/com/villo/truco/...`, tests en
`src/test/java/com/villo/truco/...`.

---

## Phase 1: Setup

**Purpose**: Línea base verde antes de tocar nada.

- [ ] T001 Confirmar baseline en verde corriendo `./gradlew test` y registrar que
  `TimeoutExactnessMatchIT` y `TimeoutReconciliationIT` pasan (servirán de red de seguridad para
  D4/D7).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Mecanismo del deadline (dominio decide ancla+asiento, emite evento derivado; app
proyecta `ancla + duración`; scheduler de fuente única). Bloquea a TODAS las user stories.

**⚠️ CRITICAL**: Ninguna user story puede empezar hasta completar esta fase.

### Dominio (eventos + ancla + emisión)

- [ ] T002 [P] Crear `ActionDeadlineSetEvent` (implements `MatchDerivedEvent`; **NO**
  `SeatTargetedEvent`, para que llegue a ambos jugadores y se reenvíe a espectadores; lleva `seat` y
  el ancla `Instant`) en
  src/main/java/com/villo/truco/domain/model/match/events/ActionDeadlineSetEvent.java
- [ ] T003 [P] Crear `ActionDeadlineClearedEvent` (implements `MatchDerivedEvent`; **NO**
  `SeatTargetedEvent`; payload vacío) en
  src/main/java/com/villo/truco/domain/model/match/events/ActionDeadlineClearedEvent.java
- [ ] T004 Modificar `Match` para recibir `Instant now` en los métodos de acción (`playCard`,
  `callTruco`, `callEnvido`, `respondTruco`, `respondEnvido`, `fold`) y al iniciar/avanzar (`start`,
  `startNewRound`, `startNewGame`), seteando `lastActivityAt = now` cuando cambia el asiento que
  debe actuar en src/main/java/com/villo/truco/domain/model/match/Match.java
- [ ] T005 En `Match`, emitir
  `ActionDeadlineSetEvent(seat = seatOf(getCurrentTurn()), ancla = lastActivityAt)` como evento
  derivado en cada transición must-act, y `ActionDeadlineClearedEvent` cuando
  `getCurrentTurn() == null` o el match deja de estar `IN_PROGRESS` (INV-3) en
  src/main/java/com/villo/truco/domain/model/match/Match.java
- [ ] T006 Verificar/confirmar en `Round` que `getCurrentTurn()` apunta al asiento que debe
  responder ante canto pendiente (D5); si no, exponer un método de "asiento que debe actuar" sin
  romper `determineTimeoutWinner` en src/main/java/com/villo/truco/domain/model/match/Round.java

### Persistencia del ancla (controlada por dominio, sin migración)

- [ ] T007 Quitar el auto-stamp de `lastActivityAt = Instant.now()` en `@PrePersist`/`@PreUpdate`
  para que persista el valor seteado por el dominio (reutiliza columna `last_activity_at`, sin
  migración) en
  src/main/java/com/villo/truco/infrastructure/persistence/entities/MatchJpaEntity.java
- [ ] T008 Verificar que `MatchMapper` propaga `lastActivityAt` del dominio hacia y desde la entidad
  sin sobrescribirlo en
  src/main/java/com/villo/truco/infrastructure/persistence/mappers/MatchMapper.java

### Threading de `now` desde la capa de aplicación

- [ ] T009 Inyectar el `Clock` existente y pasar `now = clock.instant()` a los métodos de `Match` en
  los command handlers de acción: PlayCardCommandHandler, CallTrucoCommandHandler,
  RespondTrucoCommandHandler, CallEnvidoCommandHandler, RespondEnvidoCommandHandler,
  FoldCommandHandler, StartMatchCommandHandler en
  src/main/java/com/villo/truco/application/usecases/commands/
- [ ] T010 Asegurar que la creación de partidas que arrancan inmediatamente (quick match /
  createReady) inicializa el ancla con `now` en el flujo correspondiente (
  CreateMatchCommandHandler / EnqueueForQuickMatchCommandHandler) en
  src/main/java/com/villo/truco/application/usecases/commands/

### Proyección en aplicación (deadline = ancla + duración) + fuente única

- [ ] T011 Inyectar la duración (`idleTimeout`) en `MatchEventMapper` y **componer ahí** el payload:
  `ACTION_DEADLINE_SET` →
  `{ seat, actionDeadline = ancla + idleTimeout, turnDurationMillis = idleTimeout }`,
  `ACTION_DEADLINE_CLEARED` → `{}`. La composición vive en el mapper (no en un translator) para que
  **el camino de jugador y el de espectador la obtengan gratis**, ya que ambos translators
  reutilizan el mismo mapper en
  src/main/java/com/villo/truco/application/eventhandlers/MatchEventMapper.java
- [ ] T012 Verificar (sin cambios de composición) que `ACTION_DEADLINE_SET` llega a **ambos
  jugadores** con `stateVersion = null` por ser `MatchDerivedEvent`; no requiere tocar
  `MatchNotificationEventTranslator` (línea 53 ya asigna `null` a los derivados) en
  src/main/java/com/villo/truco/application/eventhandlers/MatchNotificationEventTranslator.java
- [ ] T013 Modificar `MatchTimeoutEventHandler` para manejar el reloj por estado (fuente única, D7):
  en `IN_PROGRESS` agendar **solo** con `ACTION_DEADLINE_SET` →
  `scheduleTimeout(ancla + idleTimeout)` (ancla tomada del evento) y cancelar con
  `ACTION_DEADLINE_CLEARED`, dejando de re-agendar con otros eventos transicionales IN_PROGRESS; en
  estados pre-juego (`WAITING_FOR_PLAYERS`/`READY`) conservar el
  `scheduleTimeoutFromNow(idleTimeout)` actual en
  src/main/java/com/villo/truco/application/eventhandlers/MatchTimeoutEventHandler.java

### Tests de fundación (dominio)

- [ ] T014 [P] Tests de dominio en `MatchTest`: cada transición must-act resetea el ancla y emite
  `ActionDeadlineSetEvent`; estados sin must-act emiten `ActionDeadlineClearedEvent`; los eventos
  son derivados (no avanzan `stateVersion`) en
  src/test/java/com/villo/truco/domain/model/match/MatchTest.java
- [ ] T015 [P] Test de que mover el control del ancla al dominio no regresiona la temporalidad del
  forfeit (extender/confirmar) en
  src/test/java/com/villo/truco/integration/TimeoutExactnessMatchIT.java

**Checkpoint**: El deadline ya es autoritativo, único y se emite como derivado. Las user stories
pueden empezar.

---

## Phase 3: User Story 1 — El jugador ve cuánto tiempo le queda (Priority: P1) 🎯 MVP

**Goal**: Jugadores reciben los tres campos en el snapshot y los eventos derivados en vivo; el
forfeit ocurre en el instante mostrado.

**Independent Test**: `GET /api/matches/{id}` trae `actionDeadline`/`turnDurationMillis`/
`actionDeadlineSeat` en `roundGame`; al cambiar quién debe actuar llega `ACTION_DEADLINE_SET` sin
`stateVersion`; al vencer, el forfeit coincide (±1s).

### Tests for User Story 1 ⚠️

- [ ] T016 [P] [US1] Test de `MatchStateDTOAssembler`: arma los tres campos del deadline para el
  jugador y los deja en `null` cuando no corre reloj en
  src/test/java/com/villo/truco/application/assemblers/MatchStateDTOAssemblerTest.java
- [ ] T017 [P] [US1] Test de `MatchEventMapper`/translator: `ACTION_DEADLINE_SET` se emite a los
  jugadores con `stateVersion` ausente y payload correcto en
  src/test/java/com/villo/truco/application/eventhandlers/MatchEventMapperTest.java

### Implementation for User Story 1

- [ ] T018 [P] [US1] Agregar `actionDeadline`, `turnDurationMillis`, `actionDeadlineSeat` a
  `RoundStateDTO` en src/main/java/com/villo/truco/application/dto/RoundStateDTO.java
- [ ] T019 [US1] Poblar los tres campos en `MatchStateDTOAssembler` usando la proyección
  `ancla + idleTimeout` y `seatOf(getCurrentTurn())`; `null` cuando no corre reloj (depende de T018)
  en src/main/java/com/villo/truco/application/assemblers/MatchStateDTOAssembler.java
- [ ] T020 [P] [US1] Agregar los tres campos a `RoundStateResponse` (DTO HTTP) en
  src/main/java/com/villo/truco/infrastructure/http/dto/response/RoundStateResponse.java
- [ ] T021 [US1] Test de integración: un jugador recibe `ACTION_DEADLINE_SET`/`CLEARED` en
  `/user/queue/match` y el `MATCH_FORFEITED` coincide con `actionDeadline` (±1s) en
  src/test/java/com/villo/truco/integration/TimeoutExactnessMatchIT.java

**Checkpoint**: MVP funcional — el jugador ve y consume la cuenta regresiva fiel.

---

## Phase 4: User Story 2 — El espectador ve la misma cuenta regresiva (Priority: P2)

**Goal**: El snapshot de espectador trae los tres campos y los dos eventos derivados se reenvían a
espectadores, sin filtrar información privada.

**Independent Test**: `GET /api/matches/{id}/spectate` trae los tres campos en `currentRound`; el
espectador recibe `ACTION_DEADLINE_SET`/`CLEARED` pero no `PLAYER_HAND_UPDATED` ni
`AVAILABLE_ACTIONS_UPDATED`.

### Tests for User Story 2 ⚠️

- [ ] T022 [P] [US2] Test de `SpectatorMatchStateDTOAssembler`: arma los tres campos para el
  espectador con la misma proyección en
  src/test/java/com/villo/truco/application/assemblers/SpectatorMatchStateDTOAssemblerTest.java
- [ ] T023 [P] [US2] Test del translator de espectador: `ACTION_DEADLINE_SET`/`CLEARED` se
  reenvían (no son `SeatTargetedEvent`) y `PLAYER_HAND_UPDATED`/`AVAILABLE_ACTIONS_UPDATED` no (sí
  lo son) en
  src/test/java/com/villo/truco/application/eventhandlers/SpectatorNotificationEventTranslatorTest.java

### Implementation for User Story 2

- [ ] T024 [P] [US2] Agregar los tres campos a `SpectatorRoundStateDTO` en
  src/main/java/com/villo/truco/application/dto/SpectatorRoundStateDTO.java
- [ ] T025 [US2] Poblar los tres campos en `SpectatorMatchStateDTOAssembler` con la misma
  proyección (depende de T024) en
  src/main/java/com/villo/truco/application/assemblers/SpectatorMatchStateDTOAssembler.java
- [ ] T026 [P] [US2] Agregar los tres campos a `SpectatorRoundStateResponse` (DTO HTTP) en
  src/main/java/com/villo/truco/infrastructure/http/dto/response/SpectatorRoundStateResponse.java
- [ ] T027 [US2] Verificar (sin implementación) que `ActionDeadlineSetEvent`/`ClearedEvent` **no**
  implementan `SeatTargetedEvent`, por lo que `SpectatorNotificationEventTranslator` los reenvía
  automáticamente a espectadores y `MatchRecipientResolver` los entrega a ambos jugadores; confirmar
  que no hace falta tocar esos componentes en
  src/main/java/com/villo/truco/application/eventhandlers/SpectatorNotificationEventTranslator.java
- [ ] T028 [US2] Test de integración de espectador: snapshot `/spectate` con los tres campos y
  reenvío de los eventos derivados (nuevo IT dedicado) en
  src/test/java/com/villo/truco/integration/SpectateDeadlineIT.java

**Checkpoint**: Jugadores y espectadores ven la misma cuenta regresiva.

---

## Phase 5: User Story 3 — Fidelidad tras reconexión o reinicio (Priority: P3)

**Goal**: El deadline expuesto y ejecutado sobrevive reconexión del cliente y reinicio del servicio
apuntando al mismo instante.

**Independent Test**: Reconectar a mitad de turno → mismo `actionDeadline`; reiniciar el servicio →
plazo ejecutado y expuesto al mismo instante.

### Tests for User Story 3 ⚠️

- [ ] T029 [P] [US3] Test de reconexión: el snapshot tras reconectar devuelve el mismo
  `actionDeadline` absoluto (no reinicio) en
  src/test/java/com/villo/truco/integration/TimeoutReconciliationIT.java
- [ ] T030 [P] [US3] Test de reinicio: la reconciliación re-arma el timeout en
  `lastActivityAt + idleTimeout`, igual al deadline expuesto (SC-005) en
  src/test/java/com/villo/truco/integration/TimeoutReconciliationIT.java

### Implementation for User Story 3

- [ ] T031 [US3] Verificar/alinear `MatchTimeoutReconciliationSource` para que use exactamente la
  misma fórmula `lastActivityAt + idleTimeout` que el snapshot y el scheduler (INV-1) en
  src/main/java/com/villo/truco/infrastructure/scheduler/MatchTimeoutReconciliationSource.java
- [ ] T032 [US3] Confirmar que el ancla seteado por el dominio se persiste y rehidrata correctamente
  a través de `MatchRehydrator`/`MatchSnapshot` en
  src/main/java/com/villo/truco/domain/model/match/MatchRehydrator.java

**Checkpoint**: Todas las user stories funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentación, contrato y cierre de calidad.

- [ ] T033 [P] Corregir `docs/CONTRATOS_API.md` §9.5: mover `ACTION_DEADLINE_SET` y
  `ACTION_DEADLINE_CLEARED` de "Eventos transicionales (consumen `stateVersion`)" a una
  clasificación de eventos derivados que NO avanzan `stateVersion` (D6) en docs/CONTRATOS_API.md
- [ ] T034 [P] Verificar coherencia de §4.18, §4.14, §4.15 y §9.6 con los shapes finales (que los
  eventos no figuren como portadores de `stateVersion`) en docs/CONTRATOS_API.md
- [ ] T035 [P] Evaluar y, si corresponde, mencionar el temporizador de turno visible como capacidad
  en README.md
- [ ] T036 Ejecutar la validación de quickstart.md (manual + automatizada) en
  specs/005-turn-deadline-domain/quickstart.md
- [ ] T037 Correr `./gradlew build` y confirmar ArchUnit en verde y cobertura ≥ 70%

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA** todas las user stories.
- **User Stories (Phase 3–5)**: dependen de Foundational. US1 es el MVP. US2 y US3 pueden empezar
  tras Foundational; son independientemente testeables.
- **Polish (Phase 6)**: depende de las user stories deseadas. T033/T034 dependen de que la decisión
  de evento derivado esté implementada (Foundational).

### Dependencias dentro de Foundational

- T002, T003 (eventos) son [P] entre sí.
- T004 → T005 (emitir requiere que el ancla se setee con `now`).
- T004/T005 → T009/T010 (los handlers no pueden pasar `now` hasta que las firmas del dominio
  cambien).
- T007 → T008 (persistencia del ancla).
- T002 → T011 (el mapper compone usando el ancla que transporta el evento) y T011 → T012 (
  verificación).
- T002 → T013 (el handler toma el ancla del `ACTION_DEADLINE_SET`). T013 cierra la fuente única (
  D7).

### Within Each User Story

- Tests antes de implementación (deben fallar primero).
- DTO de aplicación → assembler → DTO HTTP.

### Parallel Opportunities

- T002 y T003 en paralelo.
- T014 y T015 en paralelo (tras T005).
- Dentro de US1: T016/T017 (tests) en paralelo; T018 y T020 en paralelo (archivos distintos).
- Dentro de US2: T022/T023 en paralelo; T024 y T026 en paralelo.
- US3 T029/T030 en paralelo.
- Polish T033/T034/T035 en paralelo.

---

## Parallel Example: Foundational

```bash
# Eventos derivados en paralelo:
Task: "Crear ActionDeadlineSetEvent en .../events/ActionDeadlineSetEvent.java"
Task: "Crear ActionDeadlineClearedEvent en .../events/ActionDeadlineClearedEvent.java"
```

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo:
Task: "Test de MatchStateDTOAssembler (campos del deadline) en .../MatchStateDTOAssemblerTest.java"
Task: "Test de MatchEventMapper (evento derivado sin stateVersion) en .../MatchEventMapperTest.java"

# DTOs en paralelo:
Task: "Agregar 3 campos a RoundStateDTO en .../dto/RoundStateDTO.java"
Task: "Agregar 3 campos a RoundStateResponse en .../http/dto/response/RoundStateResponse.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (Setup) → 2. Phase 2 (Foundational, crítico) → 3. Phase 3 (US1).
4. **STOP y VALIDAR**: jugador ve la cuenta regresiva y el forfeit coincide.

### Incremental Delivery

1. Foundational → deadline autoritativo y emitido.
2. US1 → jugador (MVP).
3. US2 → espectador.
4. US3 → fidelidad reconexión/reinicio.
5. Polish → corrección de contrato y docs (gate de cierre).

---

## Notes

- [P] = archivos distintos, sin dependencias.
- Verificar que los tests fallan antes de implementar.
- **D4/D5 son verificaciones obligatorias** (T006, T015): confirmar `getCurrentTurn()` ante canto
  pendiente y que el control del ancla no regresiona el forfeit.
- **T033 es gate de cierre**: sin corregir §9.5 el contrato con FE queda falso.
- **Composición del deadline vive en `MatchEventMapper`** (T011), no en los translators: así jugador
  y espectador comparten el payload correcto (decisión post-análisis F8).
- **Los eventos de deadline NO son `SeatTargetedEvent`** (T002/T003): el ruteo a ambos jugadores y a
  espectadores sale automático (F3); por eso T012/T027 son verificaciones, no implementación.
- **FR-010** (duración configurable) reutiliza `idleTimeout`/`MatchTimeoutProperties` existente; no
  requiere trabajo nuevo, solo inyección en T011.
- **FR-011** (referencia temporal del servidor) ya está cubierto: todo evento WS lleva `timestamp`
  en epochMillis; sin tarea de implementación.
- **T015 / T021** comparten `TimeoutExactnessMatchIT.java`: usar métodos `@Test` distintos (timing
  de forfeit vs jugador recibe eventos); el orden de fases resuelve el acoplamiento.
- Commit por tarea o grupo lógico.
