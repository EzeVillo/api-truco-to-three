---
description: "Task list para Partidas Bot vs Bot Espectables"
---

# Tasks: Partidas Bot vs Bot Espectables

**Input**: Documentos de diseño en `/specs/016-bot-vs-bot-spectating/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: incluidos por mandato de la constitución (Principio III: test-first, cobertura ≥ 70%,
títulos en español). Todos los títulos de test van en español.

## Format: `[ID] [P?] [Story] Descripción`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1, US2, US3)
- Rutas exactas en cada tarea

## Convenciones de ruta

- Código: `src/main/java/com/villo/truco/...`
- Tests: `src/test/java/com/villo/truco/...`
- Migraciones: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: el proyecto ya existe (Java 21 / Spring Boot, Clean/Hexagonal). No hay init de proyecto
ni dependencias nuevas.

- [X] T001 Verificar que la próxima migración Flyway disponible es `V24` (V22/V23 ya las tomó la
  feature 015 gameplay-recording) y que
  la feature no requiere dependencias nuevas (reusa stack actual). Sin cambios de código.

---

## Phase 2: Foundational (Registro de autoría — bloquea todas las stories)

**Purpose**: el registro `BotVsBotMatchRegistry` (matchId→ownerId) es la fuente de verdad de
ocupación, presencia, elegibilidad owner-only y veto. Todas las stories dependen de él.

**⚠️ CRITICAL**: ninguna user story puede completarse hasta terminar esta fase.

- [X] T002 [P] Crear el puerto de dominio
  `src/main/java/com/villo/truco/domain/ports/BotVsBotMatchRegistry.java` con
  `register(MatchId, PlayerId)`, `isBotVsBotMatch(MatchId): boolean`,
  `findOwnerByMatchId(MatchId): Optional<PlayerId>`,
  `findActiveOwnedMatchId(PlayerId): Optional<MatchId>` (POJO, sin Spring).
- [X] T003 [P] Crear la migración
  `src/main/resources/db/migration/V24__create_bot_vs_bot_matches.sql` con tabla
  `bot_vs_bot_matches (match_id UUID PK, owner_id UUID NOT NULL)` e índice
  `idx_bot_vs_bot_matches_owner_id`.
- [X] T004 [P] Crear la entidad JPA
  `src/main/java/com/villo/truco/infrastructure/persistence/entities/BotVsBotMatchJpaEntity.java`
  (`@Id matchId`, `ownerId`), espejando `CampaignMatchJpaEntity`.
- [X] T005 Crear el repositorio Spring Data
  `src/main/java/com/villo/truco/infrastructure/persistence/repositories/spring/SpringDataBotVsBotMatchRepository.java`
  con `existsById`, `findById`, y una query `findActiveOwnedMatchId` que cruza `bot_vs_bot_matches`
  con `matches` filtrando estados terminales (`FINISHED`, `FORFEITED`, `ABANDONED`, `CANCELLED`) por
  `owner_id` (depende de T004).
- [X] T006 Crear el adapter
  `src/main/java/com/villo/truco/infrastructure/persistence/repositories/JpaBotVsBotMatchRegistryAdapter.java`
  (`@Repository`, implementa `BotVsBotMatchRegistry`), espejando `JpaCampaignMatchRegistryAdapter`
  (depende de T002, T004, T005).
- [X] T007 [P] Test del adapter (H2)
  `src/test/java/com/villo/truco/infrastructure/persistence/repositories/JpaBotVsBotMatchRegistryAdapterTest.java`:
  `register`/`isBotVsBotMatch`/`findOwnerByMatchId`, y `findActiveOwnedMatchId` devolviendo el match
  no terminado y vacío cuando el match está finalizado (depende de T006).

**Checkpoint**: el registro de autoría persiste y consulta correctamente — las stories pueden
empezar.

---

## Phase 3: User Story 1 - Crear y espectar viendo ambas manos (Priority: P1) 🎯 MVP

**Goal**: un usuario crea una partida entre dos bots y la espectea viendo las cartas en mano de
ambos (snapshot inicial + WebSocket), siendo el único habilitado para espectarla.

**Independent Test**: `POST /api/matches/bot-vs-bot` con dos bots distintos crea la partida; el
creador recibe ambas manos por `GET /spectate` y por WS; un no-creador es rechazado; la partida no
genera revancha.

### Tests para User Story 1 ⚠️

- [X] T008 [P] [US1] Test
  `src/test/java/com/villo/truco/application/usecases/commands/CreateBotVsBotMatchCommandHandlerTest.java`:
  crea+inicia el match y registra al dueño; rechaza bots iguales; rechaza bot inexistente; rechaza
  si el creador está ocupado.
- [X] T009 [P] [US1] Extender
  `src/test/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicyTest.java`:
  bot-vs-bot permite al dueño y rechaza a un no-dueño; la rama de partidas con humanos
  (amistad/competición) queda intacta.
- [X] T010 [P] [US1] Test
  `src/test/java/com/villo/truco/application/assemblers/SpectatorMatchStateDTOAssemblerTest.java`:
  manos de ambos bots presentes en bot-vs-bot y `null` en partidas con humanos.
- [X] T011 [P] [US1] Test
  `src/test/java/com/villo/truco/application/eventhandlers/SpectatorNotificationEventTranslatorTest.java`:
  en bot-vs-bot reenvía `HAND_DEALT` (ambas manos) y `PLAYER_HAND_UPDATED` de ambos asientos; en
  partidas con humanos NO reenvía `HAND_DEALT` (cierre de fuga) ni `PLAYER_HAND_UPDATED`;
  `AVAILABLE_ACTIONS_UPDATED` nunca al espectador.
- [X] T012 [P] [US1] Test
  `src/test/java/com/villo/truco/application/services/BotVsBotRematchVetoTest.java`: vetea revancha
  si el match es bot-vs-bot; no vetea si no lo es.
- [X] T013 [US1] Test de integración
  `src/test/java/com/villo/truco/integration/BotVsBotSpectatingIT.java`: crear por REST → espectar
  como creador devuelve ambas manos → no-creador recibe `422` → la partida queda `PRIVATE` (fuera
  del
  lobby) y no abre revancha al terminar.

### Implementación — Crear partida

- [X] T014 [P] [US1] Crear
  `src/main/java/com/villo/truco/application/commands/CreateBotVsBotMatchCommand.java`
  (`ownerId`, `gamesToPlay`, `botOneId`, `botTwoId` + constructor secundario desde strings/int).
- [X] T015 [P] [US1] Crear
  `src/main/java/com/villo/truco/application/dto/CreateBotVsBotMatchDTO.java`
  (`matchId`).
- [X] T016 [P] [US1] Crear
  `src/main/java/com/villo/truco/application/ports/in/CreateBotVsBotMatchUseCase.java`
  (`extends UseCase<CreateBotVsBotMatchCommand, CreateBotVsBotMatchDTO>`).
- [X] T017 [US1] Crear
  `src/main/java/com/villo/truco/application/usecases/commands/CreateBotVsBotMatchCommandHandler.java`:
  `ensureAvailable(ownerId)`; validar bots distintos (`SamePlayerMatchException`) y existentes
  (`BotNotFoundException`); `Match.createReady(botOne, botTwo, MatchRules.fromGamesToPlay(...))`;
  `startMatch` ×2; `save`; `botVsBotMatchRegistry.register(matchId, ownerId)`; publicar eventos
  (depende de T002, T014-T016).
- [X] T018 [P] [US1] Crear
  `src/main/java/com/villo/truco/infrastructure/http/dto/request/CreateBotVsBotMatchRequest.java`
  (`@NotBlank botOneId`, `@NotBlank botTwoId`, `@NotNull @Min(1) gamesToPlay`).
- [X] T019 [P] [US1] Crear
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/CreateBotVsBotMatchResponse.java`
  (`matchId` + `from(dto)`).
- [X] T020 [US1] Agregar endpoint `POST /api/matches/bot-vs-bot` en
  `src/main/java/com/villo/truco/infrastructure/http/BotController.java` (inyecta
  `CreateBotVsBotMatchUseCase`; OpenAPI: 200/400/401/404/422) (depende de T016, T018, T019).
- [X] T021 [US1] Registrar el bean `CreateBotVsBotMatchUseCase` en
  `src/main/java/com/villo/truco/infrastructure/config/BotConfiguration.java` envuelto en
  `retryTransactionalPipeline` (depende de T017).

### Implementación — Espectar owner-only

- [X] T022 [P] [US1] Crear
  `src/main/java/com/villo/truco/domain/model/spectator/exceptions/SpectateBotMatchNotOwnerException.java`
  (`extends DomainException` → mapea a 422).
- [X] T023 [US1] Modificar
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicy.java`: si
  `registry.isBotVsBotMatch(matchId)`, omitir amistad/competición y exigir
  `findOwnerByMatchId == spectatorId` (si no, `SpectateBotMatchNotOwnerException`); conservar
  IN_PROGRESS y la guarda de "ya espectando otro match" (depende de T002, T022).

### Implementación — Ambas manos en el snapshot

- [X] T024 [US1] Modificar
  `src/main/java/com/villo/truco/application/dto/SpectatorRoundStateDTO.java`
  agregando `List<CardDTO> handPlayerOne` y `List<CardDTO> handPlayerTwo` al final.
- [X] T025 [US1] Modificar
  `src/main/java/com/villo/truco/application/assemblers/SpectatorMatchStateDTOAssembler.java`: si
  `registry.isBotVsBotMatch(matchId)`, poblar las manos con
  `match.getCardsOf(playerOne)`/`getCardsOf(playerTwo)`; si no, `null` (depende de T002, T024).

### Implementación — Ambas manos por WebSocket (+ cierre de fuga)

- [X] T026 [US1] Modificar
  `src/main/java/com/villo/truco/application/eventhandlers/SpectatorNotificationEventTranslator.java`:
  inyectar `BotVsBotMatchRegistry`; para `HandDealtEvent`/`SeatTargetedEvent` reenviar solo si es
  evento de mano (`HandDealtEvent`/`PlayerHandUpdatedEvent`) **y** `isBotVsBotMatch`; el resto de
  los
  eventos públicos sin cambios. Consultar el registro solo en esos eventos (no en el camino público)
  (depende de T002).

### Implementación — Sin revancha

- [X] T027 [P] [US1] Crear
  `src/main/java/com/villo/truco/application/services/BotVsBotRematchVeto.java`
  (`implements RematchVeto`, `vetoesRematch = registry.isBotVsBotMatch(matchId)`) (depende de T002).

### Wiring de configuración (US1)

- [X] T028 [US1] En
  `src/main/java/com/villo/truco/infrastructure/config/SpectatorConfiguration.java`,
  pasar `BotVsBotMatchRegistry` a `SpectatingEligibilityPolicy` y a
  `SpectatorMatchStateDTOAssembler`
  (depende de T023, T025).
- [X] T029 [US1] En
  `src/main/java/com/villo/truco/infrastructure/config/EventNotifierConfiguration.java`,
  pasar `BotVsBotMatchRegistry` al `SpectatorNotificationEventTranslator` (depende de T026).
- [X] T030 [US1] Registrar el bean `BotVsBotRematchVeto` (se suma a `List<RematchVeto>`) en
  `src/main/java/com/villo/truco/infrastructure/config/RematchConfiguration.java` (no en
  `MatchUseCaseConfiguration`: ahí genera un ciclo de beans con el `MatchEventNotifier`) (depende de
  T027).

**Checkpoint**: US1 funcional — crear, espectar owner-only y ver ambas manos (snapshot + WS), sin
revancha. MVP entregable.

---

## Phase 4: User Story 2 - Ocupación por autoría y presencia (Priority: P1)

**Goal**: crear una partida bot-vs-bot deja al creador **busy total** (no puede crear otra ni
iniciar
otra actividad), aunque no la espectee, y la presencia lo refleja con `ownedBotMatch`.

**Independent Test**: con una bot-match propia activa, `GET /api/me/presence` devuelve `busy:true` y
`ownedBotMatch`; crear otra bot-match / partida normal / quick / liga / copa es rechazado; sin estar
espectando.

### Tests para User Story 2 ⚠️

- [X] T031 [P] [US2] Extender
  `src/test/java/com/villo/truco/application/usecases/commands/PlayerAvailabilityCheckerTest.java`:
  `OWNS_BOT_MATCH` bloquea cuando hay bot-match propia activa; no bloquea cuando no la hay; los bots
  siguen sin chequearse.
- [X] T032 [P] [US2] Extender
  `src/test/java/com/villo/truco/application/usecases/queries/UserPresenceResolverTest.java`:
  `ownedBotMatch` presente con match activo y `busy:true`; ausente cuando no hay; no interfiere con
  los demás refs.

### Implementación — Ocupación

- [X] T033 [P] [US2] Crear
  `src/main/java/com/villo/truco/domain/model/match/exceptions/PlayerOwnsActiveBotMatchException.java`
  (`extends DomainException` → 422).
- [X] T034 [US2] Modificar
  `src/main/java/com/villo/truco/application/usecases/commands/PlayerAvailabilityChecker.java`:
  inyectar `BotVsBotMatchRegistry`; agregar `BlockingReason.OWNS_BOT_MATCH`
  (`findActiveOwnedMatchId(player).isPresent()`) y lanzar `PlayerOwnsActiveBotMatchException` en
  `ensureAvailable` (depende de T002, T033).
- [X] T035 [US2] En
  `src/main/java/com/villo/truco/infrastructure/config/PlayerAvailabilityConfiguration.java`, pasar
  `BotVsBotMatchRegistry` al `PlayerAvailabilityChecker` (depende de T034).

### Implementación — Presencia

- [X] T036 [P] [US2] Crear
  `src/main/java/com/villo/truco/application/dto/ActiveOwnedBotMatchRefDTO.java`
  (`matchId`, `status`).
- [X] T037 [US2] Modificar `src/main/java/com/villo/truco/application/dto/UserPresenceDTO.java`:
  agregar `ownedBotMatch` y sumarlo al cálculo de `busy` en `of(...)` (depende de T036).
- [X] T038 [US2] Modificar
  `src/main/java/com/villo/truco/application/usecases/queries/UserPresenceResolver.java`: inyectar
  `BotVsBotMatchRegistry`; resolver `ownedBotMatch` vía `findActiveOwnedMatchId(player)` + estado
  del
  match (`MatchQueryRepository`) (depende de T002, T037).
- [X] T039 [US2] Modificar
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/UserPresenceResponse.java`:
  agregar
  el record anidado `OwnedBotMatchRef { matchId, status }`, el campo `ownedBotMatch` y su mapeo en
  `from(...)` (depende de T037).
- [X] T040 [US2] En
  `src/main/java/com/villo/truco/infrastructure/config/PresenceUseCaseConfiguration.java`,
  pasar `BotVsBotMatchRegistry` al `UserPresenceResolver` (depende de T038).
- [X] T041 [US2] Test de integración
  `src/test/java/com/villo/truco/integration/BotVsBotOccupancyIT.java`: tras crear una bot-match,
  `GET /api/me/presence` muestra `busy:true` + `ownedBotMatch` sin espectar; crear otra bot-match y
  una partida normal devuelven `422` (depende de T034, T039, T040).

**Checkpoint**: US1 + US2 funcionan — la autoría ocupa al creador y la presencia lo refleja.

---

## Phase 5: User Story 3 - Liberación al terminar (Priority: P2)

**Goal**: cuando la partida bot-vs-bot termina, el creador queda libre automáticamente (la ocupación
por autoría deja de estar activa) y puede crear otra.

**Independent Test**: al llegar el match a `FINISHED`, `findActiveOwnedMatchId` deja de devolverlo →
presencia `busy:false`, `ownedBotMatch:null`, y se puede crear una nueva partida.

### Tests para User Story 3 ⚠️

- [X] T042 [US3] Test de integración
  `src/test/java/com/villo/truco/integration/BotVsBotReleaseIT.java`: dejar que una bot-match llegue
  a estado terminal y verificar que `GET /api/me/presence` pasa a `busy:false`/`ownedBotMatch:null`
  y
  que el creador puede crear una nueva bot-match (depende de T034, T038, Phase 2).

> La liberación es emergente del filtro de estado en `findActiveOwnedMatchId` (T005) y no requiere
> código de producción adicional: US3 valida ese comportamiento de punta a punta.

**Checkpoint**: las tres stories funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting

- [X] T043 [P] Actualizar `docs/CONTRATOS_API.md`: nuevo `POST /api/matches/bot-vs-bot`; manos
  (`handPlayerOne`/`handPlayerTwo`) en `GET /spectate` solo bot-vs-bot; `ownedBotMatch` en
  `GET /api/me/presence`; sección spectate WS (HAND_DEALT/PLAYER_HAND_UPDATED de ambos asientos en
  bot-vs-bot y cierre de fuga de HAND_DEALT para partidas con humanos); espectado owner-only.
- [X] T044 [P] Revisar `README.md` y actualizar si lista recursos REST / capacidades / enums (
  agregar
  el recurso bot-vs-bot y la ocupación por autoría si corresponde).
- [X] T045 Verificar `CleanArchitectureTest` (ArchUnit) en verde tras los cambios de capas y
  puertos.
- [X] T046 Ejecutar `./gradlew build` (cobertura JaCoCo ≥ 70%) y `./gradlew test`; corregir lo que
  falle.
- [X] T047 Ejecutar la validación de `quickstart.md` de punta a punta (crear, ocupar, espectar ambas
  manos por WS, no-regresión de fuga en partidas con humanos, liberar).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: tras Setup. **BLOQUEA** todas las stories (el registro es
  transversal).
- **US1 (Phase 3)**, **US2 (Phase 4)**, **US3 (Phase 5)**: tras Phase 2.
- **Polish (Phase 6)**: tras las stories deseadas.

### User Story Dependencies

- **US1 (P1)**: solo depende de Phase 2. Es el MVP.
- **US2 (P1)**: solo depende de Phase 2. Independiente de US1 (puede integrarse con la creación de
  US1 para los tests de integración, pero la ocupación/presencia es testeable por separado).
- **US3 (P2)**: depende de Phase 2 (filtro de estado) y reusa ocupación/presencia de US2 para sus
  asserts; sin código de producción nuevo.

### Within Each User Story

- Los tests se escriben primero y deben fallar antes de implementar.
- DTOs/commands/puertos antes que el handler; handler antes que el wiring; wiring antes que la
  integración.
- Tareas que tocan el **mismo archivo** no son `[P]` (ej. T028 sobre `SpectatorConfiguration`).

### Parallel Opportunities

- Phase 2: T002, T003, T004 en paralelo; luego T005→T006; T007 tras T006.
- US1 tests (T008-T012) en paralelo; T014-T016, T018-T019, T022, T027 en paralelo.
- US2: T031/T032 en paralelo; T033/T036 en paralelo.

---

## Parallel Example: User Story 1 (tests primero)

```bash
Task: "T008 CreateBotVsBotMatchCommandHandlerTest"
Task: "T009 SpectatingEligibilityPolicyTest (bot-vs-bot)"
Task: "T010 SpectatorMatchStateDTOAssemblerTest"
Task: "T011 SpectatorNotificationEventTranslatorTest"
Task: "T012 BotVsBotRematchVetoTest"
```

---

## Implementation Strategy

### MVP First (US1)

1. Phase 1 (Setup) → Phase 2 (registro de autoría) → Phase 3 (US1).
2. **STOP & VALIDATE**: crear y espectar una partida bot-vs-bot viendo ambas manos.

### Incremental Delivery

1. Setup + Foundational → base lista.
2. US1 → crear + espectar ambas manos (MVP).
3. US2 → ocupación por autoría + presencia.
4. US3 → liberación al terminar.
5. Polish → docs, ArchUnit, build/cobertura, quickstart.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes.
- Las nuevas excepciones extienden `DomainException` → 422 automático vía `GlobalExceptionHandler`
  (sin tocar el handler).
- El adapter del registro es `@Repository` (bean automático); el resto del wiring inyecta el puerto
  `BotVsBotMatchRegistry` en las `@Configuration` listadas.
- Mantener títulos de test en español (constitución, Principio III y IV).
- Commitear tras cada tarea o grupo lógico.
