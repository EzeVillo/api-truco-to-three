# Tasks: Versionado de Match para Reconciliación Snapshot + Stream

**Input**: Design documents from `/specs/004-match-versioning-snapshot/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md,
data-model.md, contracts/

**Tests**: Tests are included as explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing
of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migración de base de datos y preparación del proyecto para la feature.

- [X] T001 Crear migración Flyway `VXXX__add_match_state_version.sql` en
  `src/main/resources/db/migration/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura de dominio, aplicación e infraestructura compartida que DEBE estar
completa antes de que cualquier user story pueda implementarse.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Agregar campo `stateVersion` a `AggregateBase.java` en
  `src/main/java/com/villo/truco/domain/shared/`
- [X] T003 [P] Agregar campo `stateVersion` y getter a `MatchDomainEvent.java` en
  `src/main/java/com/villo/truco/domain/model/match/events/`
- [X] T004 [P] Propagar `stateVersion` en `MatchEventEnvelope.java` en
  `src/main/java/com/villo/truco/domain/model/match/events/`
- [X] T005 [P] Agregar campo `stateVersion` a `MatchSnapshot.java` y propagar en
  `MatchSnapshotExtractor.java` en `src/main/java/com/villo/truco/domain/model/match/`
- [X] T006 [P] Agregar columna `state_version` a `MatchJpaEntity.java` en
  `src/main/java/com/villo/truco/infrastructure/persistence/entities/`
- [X] T007 [P] Propagar `stateVersion` en `MatchMapper.java` en
  `src/main/java/com/villo/truco/infrastructure/persistence/mappers/` (ambas direcciones)
- [X] T008 [P] Agregar `stateVersion` a `MatchStateDTO.java`, `SpectatorMatchStateDTO.java` y
  `MatchStateDTOAssembler.java` en `src/main/java/com/villo/truco/application/dto/` y
  `src/main/java/com/villo/truco/application/assemblers/`
- [X] T009 [P] Agregar `stateVersion` a `MatchStateResponse.java` y
  `SpectatorMatchStateResponse.java` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/`
- [X] T010 [P] Agregar `stateVersion` nullable a `MatchWsEvent.java` en
  `src/main/java/com/villo/truco/infrastructure/websocket/dto/`
- [X] T011 [P] Agregar `stateVersion` nullable a `MatchEventNotification.java` en
  `src/main/java/com/villo/truco/application/events/`
- [X] T012 Refactorizar `Match.java` para incrementar `stateVersion` en eventos transicionales
  mediante `addTransitionalEvent()` y preservar contador en `addDerivedEvent()` en
  `src/main/java/com/villo/truco/domain/model/match/`
- [X] T013 Actualizar `Match.reconstruct()` para aceptar y restaurar `stateVersion` en
  `src/main/java/com/villo/truco/domain/model/match/`
- [X] T014 Actualizar `MatchNotificationEventTranslator.java` para propagar `stateVersion` desde
  eventos de dominio a notificaciones en `src/main/java/com/villo/truco/application/eventhandlers/`
- [X] T015 Implementar test de dominio `MatchStateVersionTest.java` en
  `src/test/java/com/villo/truco/domain/model/match/` (verifica monotonía, incremento por transición
  y reconstrucción)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel.

---

## Phase 3: User Story 1 - Conexión inicial al match sin perder ni duplicar eventos (Priority: P1) 🎯 MVP

**Goal**: El snapshot REST y el stream WS incluyen `stateVersion`, permitiendo al cliente
reconciliar el estado inicial con eventos posteriores.

**Independent Test**: `GET /api/matches/{matchId}` devuelve `stateVersion` consistente con el número
de transiciones aplicadas; suscripción a `/user/queue/match` recibe eventos con `stateVersion`
estrictamente consecutivo.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T016 [P] [US1] Implementar test de integración REST `MatchControllerSnapshotVersionTest.java`
  en `src/test/java/com/villo/truco/infrastructure/http/`
- [X] T017 [P] [US1] Implementar test de integración WS `MatchWsStreamVersionTest.java` en
  `src/test/java/com/villo/truco/infrastructure/websocket/`

### Implementation for User Story 1

- [X] T018 [US1] Actualizar `StompMatchNotificationHandler.java` para entregar eventos
  transicionales con `stateVersion` a `/user/queue/match` en
  `src/main/java/com/villo/truco/infrastructure/websocket/`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently.

---

## Phase 4: User Story 2 - Reconexión tras pérdida de socket sin desincronizarse (Priority: P1)

**Goal**: El cliente puede detectar huecos por saltos en `stateVersion` y resincronizar mediante
snapshot sin duplicaciones ni omitir eventos.

**Independent Test**: Simular desconexión/reconexión WS durante un match en curso; verificar que el
cliente detecta hueco por salto en `stateVersion`, recupera snapshot y continúa aplicando eventos en
vivo correctamente.

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T019 [P] [US2] Crear test de integración WS `MatchWsReconnectionTest.java` en
  `src/test/java/com/villo/truco/infrastructure/websocket/` (simula desconexión, hueco y
  resincronización)

### Implementation for User Story 2

> No hay código servidor nuevo específico de esta historia; se apoya en la infraestructura de
`stateVersion` de la Fase 2 y la entrega transicional de la US1.

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently.

---

## Phase 5: User Story 3 - Eventos con información oculta sin romper la secuencia global (Priority: P1)

**Goal**: El reparto de cartas se modela como transición única `HandDealtEvent` con mismo
`stateVersion` y payload redactado por jugador, sin filtración indirecta.

**Independent Test**: Todos los jugadores del match reciben evento `HAND_DEALT` con el *
*mismo `stateVersion`** y **mismo `eventType`**, pero cada uno ve únicamente sus cartas;
espectadores reciben payload vacío.

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T020 [P] [US3] Crear test `MatchNotificationEventTranslatorTest.java` verificando redacción
  por jugador de `HandDealtEvent` y mismo `stateVersion` entre variantes en
  `src/test/java/com/villo/truco/application/eventhandlers/`

### Implementation for User Story 3

- [X] T021 [P] [US3] Crear evento de dominio `HandDealtEvent.java` en
  `src/main/java/com/villo/truco/domain/model/match/events/`
- [X] T022 [US3] Modificar `Match.java` para emitir `HandDealtEvent` como transicional en lugar de
  `PlayerHandUpdatedEvent` durante el reparto en `src/main/java/com/villo/truco/domain/model/match/`
- [X] T023 [US3] Actualizar `MatchNotificationEventTranslator.java` para redactar payload por
  jugador de `HandDealtEvent` manteniendo mismo `stateVersion` en
  `src/main/java/com/villo/truco/application/eventhandlers/`

**Checkpoint**: User Story 3 independently functional.

---

## Phase 6: User Story 4 - Notificaciones no transicionales no consumen número de transición (Priority: P2)

**Goal**: `PlayerHandUpdatedEvent` y `AvailableActionsUpdatedEvent` se reclasifican como derivados,
se entregan por `/user/queue/match-derived` sin `stateVersion`, y no generan huecos artificiales en
la secuencia transicional.

**Independent Test**: Eventos derivados no avanzan `stateVersion`; jugadores no observan huecos;
canal separado recibe notificaciones sin campo `stateVersion`.

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T024 [P] [US4] Crear test `MatchWsDerivedChannelTest.java` verificando canal
  `/user/queue/match-derived` y ausencia de `stateVersion` en
  `src/test/java/com/villo/truco/infrastructure/websocket/`

### Implementation for User Story 4

- [X] T025 [P] [US4] Crear marker interface `MatchDerivedEvent.java` en
  `src/main/java/com/villo/truco/domain/model/match/events/`
- [X] T026 [US4] Reclasificar `PlayerHandUpdatedEvent` e `AvailableActionsUpdatedEvent` como
  derivados implementando `MatchDerivedEvent` en
  `src/main/java/com/villo/truco/domain/model/match/events/`
- [X] T027 [US4] Actualizar `MatchNotificationEventTranslator.java` para separar eventos
  transicionales de derivados y asignar `null` a `stateVersion` en derivados en
  `src/main/java/com/villo/truco/application/eventhandlers/`
- [X] T028 [US4] Actualizar `StompMatchNotificationHandler.java` para rutear derivados a
  `/user/queue/match-derived` sin `stateVersion` en
  `src/main/java/com/villo/truco/infrastructure/websocket/`
- [X] T029 [US4] Ampliar `MatchNotificationEventTranslatorTest.java` para verificar separación
  transicional vs derivado en `src/test/java/com/villo/truco/application/eventhandlers/`

**Checkpoint**: All user stories should now be independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentación, cobertura y validación end-to-end de todas las historias.

- [X] T030 [P] Actualizar `docs/CONTRATOS_API.md` con campo `stateVersion` en snapshot REST y stream
  WS, nuevo destino `/user/queue/match-derived`, y reglas de reconciliación cliente
- [X] T031 [P] Actualizar `README.md` con capacidad de reconciliación snapshot + stream y canales WS
- [X] T032 [P] Actualizar `CLAUDE.md` para apuntar a `specs/004-match-versioning-snapshot/plan.md`
- [X] T033 Ejecutar `./gradlew build` y verificar cobertura JaCoCo >= 70%
- [X] T034 Ejecutar validación manual según `specs/004-match-versioning-snapshot/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
    - User stories can then proceed in parallel (if staffed).
    - Or sequentially in priority order (P1 → P1 → P1 → P2).
- **Polish (Final Phase)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories.
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Uses mechanism from US1 but is
  independently testable.
- **User Story 3 (P1)**: Can start after Foundational (Phase 2) - Introduce `HandDealtEvent`; no
  dependency on US1/US2.
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) - Reclassifica eventos existentes;
  no dependency on US1/US2/US3.

### Within Each User Story

- Tests MUST be written and FAIL before implementation (where compilation allows).
- Models before services.
- Services before endpoints/handlers.
- Core implementation before integration.
- Story complete before moving to next priority.

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel.
- All Foundational tasks marked [P] can run in parallel (within Phase 2).
- Once Foundational phase completes, all user stories can start in parallel (if team capacity
  allows).
- All tests for a user story marked [P] can run in parallel.
- Different user stories can be worked on in parallel by different team members.

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Implementar test de integración REST MatchControllerSnapshotVersionTest.java"
Task: "Implementar test de integración WS MatchWsStreamVersionTest.java"

# After tests are written and failing, implement:
Task: "Actualizar StompMatchNotificationHandler.java para entregar stateVersion en /user/queue/match"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (migración Flyway).
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories).
3. Complete Phase 3: User Story 1 (snapshot + stream básico con `stateVersion`).
4. **STOP and VALIDATE**: Test User Story 1 independently.
5. Deploy/demo if ready.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready.
2. Add User Story 1 → Test independently → Deploy/Demo (MVP: reconciliación inicial funciona).
3. Add User Story 2 → Test independently → Deploy/Demo (reconexión confiable).
4. Add User Story 3 → Test independently → Deploy/Demo (información oculta preserva secuencia).
5. Add User Story 4 → Test independently → Deploy/Demo (canal derivado sin contaminar secuencia).
6. Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together.
2. Once Foundational is done:
    - Developer A: User Story 1
    - Developer B: User Story 2
    - Developer C: User Story 3
    - Developer D: User Story 4
3. Stories complete and integrate independently.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks.
- [Story] label maps task to specific user story for traceability.
- Each user story should be independently completable and testable.
- Verify tests fail before implementing.
- Commit after each task or logical group.
- Stop at any checkpoint to validate story independently.
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence.
