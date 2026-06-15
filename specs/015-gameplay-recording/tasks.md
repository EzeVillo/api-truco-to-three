---
description: "Task list for feature implementation: Recopilación de partidas para entrenamiento de bots"
---

# Tasks: Recopilación de partidas para entrenamiento de bots

**Input**: Design documents from `/specs/015-gameplay-recording/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: INCLUDED. La feature exige Test-First ≥70% (Constitution III) y el plan/quickstart listan
explícitamente tests del decorator, del factory, del adapter (H2) e integridad/idempotencia.

**Organization**: Tareas agrupadas por user story para implementación y verificación independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo (archivo distinto, sin dependencias incompletas)
- **[Story]**: User story a la que pertenece (US1, US2, US3)
- Rutas exactas incluidas en cada descripción

## Path Conventions

Backend único Hexagonal + DDD: código en `src/main/java/com/villo/truco/`, tests en
`src/test/java/com/villo/truco/`, migraciones en `src/main/resources/db/migration/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar baseline verde y anclar el número de migración antes de tocar código.

- [X] T001 Ejecutar `./gradlew test` y confirmar suite verde (incl. `CleanArchitectureTest` y JaCoCo ≥70%) como línea base
- [X] T002 Confirmar que la última migración Flyway es `V21__campaign_unlocked_casual_bots.sql` en `src/main/resources/db/migration/`, de modo que la nueva sea `V22` (reverificar contra la branch antes de crear el archivo)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: VOs de dominio, enums, puerto de salida y marker de application que TODAS las stories necesitan.

**⚠️ CRITICAL**: Ninguna user story puede empezar hasta completar esta fase.

- [X] T003 [P] Crear enum `RecordedActionType` (`PLAY_CARD`, `CALL_TRUCO`, `RESPOND_TRUCO`, `CALL_ENVIDO`, `RESPOND_ENVIDO`, `FOLD`) en `src/main/java/com/villo/truco/domain/model/gameplay/RecordedActionType.java`
- [X] T004 [P] Crear enum `ActorType` (`HUMAN`, `BOT`) en `src/main/java/com/villo/truco/domain/model/gameplay/ActorType.java`
- [X] T005 [P] Crear enum `ActorSeat` (`PLAYER_ONE`, `PLAYER_TWO`) en `src/main/java/com/villo/truco/domain/model/gameplay/ActorSeat.java`
- [X] T006 [P] Crear VO `RecordedAction` (`type: RecordedActionType` + `detail` mínimo nullable) en `src/main/java/com/villo/truco/domain/model/gameplay/RecordedAction.java` (usa T003)
- [X] T007 Crear VO `RecordedDecision` (matchId, stateVersion, gameNumber, roundNumber, actorSeat, actorType, action, snapshot `MatchSnapshot`, occurredAt, schemaVersion) en `src/main/java/com/villo/truco/domain/model/gameplay/RecordedDecision.java` (usa T004, T005, T006 y `MatchSnapshot`)
- [X] T008 Crear puerto de salida `GameplayRecorderPort` (`void record(RecordedDecision)`) en `src/main/java/com/villo/truco/domain/ports/GameplayRecorderPort.java` (usa T007)
- [X] T009 Crear marker interface `MatchActionCommand` (`MatchId matchId()`, `PlayerId playerId()`) en `src/main/java/com/villo/truco/application/commands/MatchActionCommand.java`
- [X] T010 Hacer que los 6 commands implementen `MatchActionCommand` (`implements MatchActionCommand`, ya exponen `matchId()`/`playerId()`) en `PlayCardCommand.java`, `CallTrucoCommand.java`, `RespondTrucoCommand.java`, `CallEnvidoCommand.java`, `RespondEnvidoCommand.java`, `FoldCommand.java` de `src/main/java/com/villo/truco/application/commands/` (usa T009)

**Checkpoint**: Dominio + puerto + marker listos — las user stories pueden comenzar.

---

## Phase 3: User Story 1 - Registrar cada decisión jugable con su estado (Priority: P1) 🎯 MVP

**Goal**: Capturar de forma transparente, post-commit y en transacción propia, el `MatchSnapshot` resultante + la acción de cada una de las 6 jugadas (humano y bot), persistiéndolo append-only en `match_action_log`.

**Independent Test**: Jugar una partida completa contra un bot y verificar en `match_action_log` una fila por cada acción (propia y del bot), con estado completo, actor y acción; sin huecos ni duplicados; sin impacto en la jugada.

### Tests for User Story 1 ⚠️

> Escribir estos tests PRIMERO y verificar que FALLAN antes de implementar.

- [X] T011 [P] [US1] Test de `RecordedActionFactory`: mapea cada uno de los 6 commands a su `RecordedActionType` + detalle, en `src/test/java/com/villo/truco/application/usecases/recording/RecordedActionFactoryTest.java`
- [X] T012 [P] [US1] Test de `GameplayRecordingDecorator`: registra tras `handle` exitoso; deriva `actorType` (vía `BotRegistry`) y `actorSeat` (comparando con `snapshot`); NO registra si el delegate lanza; **traga y loguea** si el puerto lanza, en `src/test/java/com/villo/truco/application/usecases/recording/GameplayRecordingDecoratorTest.java`
- [X] T013 [P] [US1] Test (mock-based, estilo `JpaMatchRepositoryAdapterTest`) de `JpaGameplayRecorderAdapter`: mapea estado/acción a JSONB y persiste append-only, en `src/test/java/com/villo/truco/infrastructure/persistence/repositories/JpaGameplayRecorderAdapterTest.java`

### Implementation for User Story 1

- [X] T014 [P] [US1] Implementar `RecordedActionFactory` (`from(MatchActionCommand) -> RecordedAction`, switch por tipo) en `src/main/java/com/villo/truco/application/usecases/recording/RecordedActionFactory.java`
- [X] T015 [P] [US1] Crear migración `V22__create_match_action_log.sql` (tabla append-only con columnas JSONB `match_state`/`action_detail`, `schema_version`, `UNIQUE(match_id, state_version)`) en `src/main/resources/db/migration/V22__create_match_action_log.sql`
- [X] T016 [P] [US1] Crear entidad JPA `MatchActionLogJpaEntity` (mapea la tabla; `match_state`/`action_detail` como JSONB al estilo `MatchJpaEntity.current_round`) en `src/main/java/com/villo/truco/infrastructure/persistence/entities/MatchActionLogJpaEntity.java`
- [X] T017 [US1] Crear `SpringDataMatchActionLogRepository` (Spring Data, con `existsByMatchIdAndStateVersion`) en `src/main/java/com/villo/truco/infrastructure/persistence/repositories/spring/SpringDataMatchActionLogRepository.java` (usa T016)
- [X] T018 [US1] Implementar `JpaGameplayRecorderAdapter` (`@Transactional` nueva tx; idempotencia por chequeo de existencia + `UNIQUE(match_id, state_version)` como garantía dura, portable a H2/Postgres; serializa snapshot/acción a JSONB) en `src/main/java/com/villo/truco/infrastructure/persistence/repositories/JpaGameplayRecorderAdapter.java` (usa T008, T016, T017)
- [X] T019 [US1] Implementar `GameplayRecordingDecorator` (`UseCase<C extends MatchActionCommand, MatchId>`: delega→commit, re-lee vía `MatchQueryRepository`, extrae `MatchSnapshot` con `MatchSnapshotExtractor`, deriva actor con `BotRegistry`, arma `RecordedDecision` y llama al puerto en try/catch) en `src/main/java/com/villo/truco/application/usecases/recording/GameplayRecordingDecorator.java` (usa T008, T014, T018)
- [X] T020 [US1] Cablear el decorator POR FUERA del pipeline sobre los 6 beans (`recordingDecorator.decorate(retryTransactionalPipeline.wrap(handler))::handle`) para `playCard`, `callTruco`, `respondTruco`, `callEnvido`, `respondEnvido`, `fold` en `src/main/java/com/villo/truco/infrastructure/config/MatchUseCaseConfiguration.java` (usa T019)
- [X] T021 [US1] Correr `./gradlew test --tests "*GameplayRecordingDecoratorTest" --tests "*RecordedActionFactoryTest" --tests "*JpaGameplayRecorderAdapterTest" --tests "*CleanArchitectureTest"` y confirmar verde

**Checkpoint**: US1 funcional — cada jugada (humana y de bot) queda registrada sin alterar el juego. MVP entregable.

---

## Phase 4: User Story 2 - Exportar/consultar el historial recopilado (Priority: P2)

**Goal**: Garantizar que lo recopilado es consultable y autocontenido fuera del juego. Por decisión del usuario NO se construye endpoint/CLI/serializador: el acceso es por SQL directo, restringido a admin/operador (acceso a base).

**Independent Test**: Con varias partidas registradas, ejecutar la consulta SQL de exportación y verificar una entrada por decisión, cada una autocontenida (estado + acción + actor + metadatos).

- [X] T022 [US2] Documentar el acceso por SQL (una fila por decisión, autocontenida) y la restricción admin/operador de acceso a la base en `README.md` y enlazar a las consultas de `specs/015-gameplay-recording/quickstart.md`
- [ ] T023 [US2] Validar manualmente con `quickstart.md` que la consulta `SELECT ... FROM match_action_log WHERE match_id = ... ORDER BY state_version` devuelve una entrada por decisión con estado completo (ambas manos), acción y actor

**Checkpoint**: Los datos recopilados son extraíbles y autocontenidos vía SQL (FR-011/FR-012 satisfechos sin tooling).

---

## Phase 5: User Story 3 - Integridad y durabilidad perpetua del registro (Priority: P3)

**Goal**: Asegurar que los registros son inmutables, idempotentes, versionados por esquema y conservados sin borrado.

**Independent Test**: Verificar que filas ya escritas no cambian al avanzar la partida, que reintentos con misma `(match_id, state_version)` no duplican, y que cada fila declara `schema_version`.

### Tests for User Story 3 ⚠️

- [X] T024 [US3] Agregar al test del adapter las aserciones de idempotencia (`ON CONFLICT DO NOTHING` no duplica ante misma `(match_id, state_version)`) e inmutabilidad (filas previas no se modifican), y que `schema_version` queda persistido, en `src/test/java/com/villo/truco/infrastructure/persistence/adapters/JpaGameplayRecorderAdapterTest.java`

### Implementation for User Story 3

- [X] T025 [US3] Definir `schemaVersion` constante = 1 al construir `RecordedDecision` en `src/main/java/com/villo/truco/application/usecases/recording/GameplayRecordingDecorator.java` y confirmar que el adapter es solo-INSERT (sin UPDATE/DELETE) en `src/main/java/com/villo/truco/infrastructure/persistence/adapters/JpaGameplayRecorderAdapter.java` (append-only, FR-007/FR-008)

**Checkpoint**: Dataset inmutable, idempotente y versionado.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentación, gates de arquitectura/cobertura y validación end-to-end.

- [X] T026 [P] Actualizar `README.md` con la tabla `match_action_log` y la capacidad "registro de partidas para entrenamiento de bots" (si el README lista tablas/capacidades)
- [X] T027 [P] Agregar nota en `AGENTS.md` ("Decisiones de dominio y patrones clave") sobre el registro post-commit vía decorator externo al pipeline transaccional
- [X] T028 Correr `./gradlew test` completo y confirmar `CleanArchitectureTest` (ArchUnit) verde y JaCoCo ≥70%
- [ ] T029 Ejecutar la validación end-to-end de `specs/015-gameplay-recording/quickstart.md` (jugar partida vs bot y verificar el registro por SQL)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias — arranca de inmediato
- **Foundational (Phase 2)**: depende de Setup — BLOQUEA todas las user stories
- **User Stories (Phase 3-5)**: dependen de Foundational
  - US1 (P1) es la base de captura; US3 (P3) endurece lo que US1 persiste; US2 (P2) consume lo persistido
- **Polish (Phase 6)**: depende de las stories deseadas completas

### User Story Dependencies

- **US1 (P1)**: arranca tras Foundational — sin dependencias de otras stories
- **US2 (P2)**: requiere datos ya persistidos por US1 para validar la consulta (la doc puede escribirse en paralelo)
- **US3 (P3)**: refuerza la persistencia de US1 (idempotencia/inmutabilidad/schema_version); su test extiende el adapter de US1

### Within Each User Story

- Tests escritos y en FALLO antes de implementar
- VOs/enums antes que factory/decorator
- Migración + entidad + repositorio antes que el adapter
- Adapter + decorator antes del wiring en la `@Configuration`

### Parallel Opportunities

- T003, T004, T005, T006 (enums + `RecordedAction`) en paralelo (archivos distintos)
- Tests de US1 (T011, T012, T013) en paralelo
- Dentro de US1: T014 (factory), T015 (migración), T016 (entidad) en paralelo; el adapter (T018) espera entidad+repo
- Polish: T026 y T027 en paralelo

---

## Parallel Example: User Story 1

```bash
# Tests de US1 juntos (deben fallar primero):
Task: "RecordedActionFactoryTest en src/test/.../application/usecases/recording/RecordedActionFactoryTest.java"
Task: "GameplayRecordingDecoratorTest en src/test/.../application/usecases/recording/GameplayRecordingDecoratorTest.java"
Task: "JpaGameplayRecorderAdapterTest en src/test/.../infrastructure/persistence/adapters/JpaGameplayRecorderAdapterTest.java"

# Implementación inicial en paralelo:
Task: "RecordedActionFactory en application/usecases/recording/RecordedActionFactory.java"
Task: "V22__create_match_action_log.sql en src/main/resources/db/migration/"
Task: "MatchActionLogJpaEntity en infrastructure/persistence/entities/"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (Setup) → 2. Phase 2 (Foundational, CRÍTICO) → 3. Phase 3 (US1)
4. **PARAR y VALIDAR**: jugar una partida vs bot y confirmar el registro por SQL
5. Desplegar/demo: desde ese momento toda partida queda guardada

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → captura funcionando → MVP
3. US3 → endurecer integridad/idempotencia/versionado
4. US2 → documentar/validar consulta SQL de exportación
5. Polish → docs + gates + e2e

---

## Notes

- [P] = archivos distintos, sin dependencias incompletas
- El decorator va POR FUERA del `retryTransactionalPipeline` para correr post-commit en tx propia (FR-009/FR-010)
- No se decora `ExecuteBotTurnUseCase` (orquestador): el bot ya pasa por los 6 beans atómicos
- Sin endpoint/CLI/export (recorte del usuario, D10): acceso por SQL directo
- Commit tras cada tarea o grupo lógico; mantener `CleanArchitectureTest` y JaCoCo ≥70% verdes
