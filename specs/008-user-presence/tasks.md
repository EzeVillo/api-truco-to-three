---
description: "Lista de tareas para la feature 008-user-presence"
---

# Tasks: Estado de presencia / ocupación del usuario

**Input**: Documentos de diseño en `/specs/008-user-presence/`

**Prerequisites**: [plan.md](plan.md) (requerido), [spec.md](spec.md) (historias de usuario),
[research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)

**Tests**: INCLUIDOS. El Principio III de la constitution (Test-First con coverage mínimo 70%) exige
que todo código nuevo esté acompañado de tests. Títulos de tests en español.

**Organization**: Las tareas se agrupan por historia de usuario para permitir implementación y
testeo independientes de cada incremento.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: Historia de usuario a la que pertenece la tarea (US1, US2, US3)
- Cada tarea incluye la ruta de archivo exacta

## Path Conventions

Monolito modular Hexagonal: `src/main/java/com/villo/truco/...` y
`src/test/java/com/villo/truco/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Orientación sobre los patrones existentes a replicar. No se requieren nuevas
dependencias, paquetes ni migraciones.

- [X] T001 Revisar los archivos de referencia del patrón query existente para replicar convenciones:
  `application/queries/GetRematchSessionQuery.java`,
  `application/ports/in/GetRematchSessionUseCase.java`,
  `application/usecases/queries/GetRematchSessionQueryHandler.java`,
  `infrastructure/http/RematchController.java` y
  `infrastructure/config/RematchConfiguration.java`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Esqueleto compartido del endpoint que las tres historias completan. Al terminar esta
fase, `GET /api/me/presence` responde `busy: false` con las cuatro referencias en `null` y rechaza
peticiones sin autenticación.

**⚠️ CRITICAL**: Ninguna historia de usuario puede empezar hasta completar esta fase.

- [X] T002 [P] Crear el record `GetUserPresenceQuery(PlayerId requester)` con `requireNonNull` en
  `src/main/java/com/villo/truco/application/queries/GetUserPresenceQuery.java` (incluir constructor
  de conveniencia `(String requester)` que haga `PlayerId.of`).
- [X] T003 [P] Crear el puerto de entrada
  `GetUserPresenceUseCase extends UseCase<GetUserPresenceQuery, UserPresenceDTO>` en
  `src/main/java/com/villo/truco/application/ports/in/GetUserPresenceUseCase.java`.
- [X] T004 [P] Crear el DTO agregador `UserPresenceDTO` (campos `busy`, `match`, `league`, `cup`,
  `rematch`, refs nullable) con factory estática
  `of(ActiveMatchRefDTO, ActiveLeagueRefDTO, ActiveCupRefDTO, ActiveRematchRefDTO)` que deriva
  `busy = match != null || league != null || cup != null || rematch != null` (FR-007) en
  `src/main/java/com/villo/truco/application/dto/UserPresenceDTO.java`.
- [X] T005 Crear `GetUserPresenceQueryHandler implements GetUserPresenceUseCase` con constructor que
  inyecta `MatchQueryRepository`, `LeagueQueryRepository`, `CupQueryRepository` y
  `RematchSessionRepository` (con `requireNonNull`) en
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T006 Crear el DTO de respuesta HTTP `UserPresenceResponse` (campo `busy` + 4 sub-refs
  nullable)
  con método estático `from(UserPresenceDTO)` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/UserPresenceResponse.java`.
- [X] T007 Crear `PresenceController` con `GET /api/me/presence`, `@AuthenticationPrincipal Jwt`,
  que invoca `getUserPresence.handle(new GetUserPresenceQuery(jwt.getSubject()))` y retorna
  `UserPresenceResponse.from(...)`; anotaciones Swagger 200/401 en
  `src/main/java/com/villo/truco/infrastructure/http/PresenceController.java`.
- [X] T008 Crear `PresenceUseCaseConfiguration` con el `@Bean GetUserPresenceUseCase` que construye
  el handler inyectando los cuatro repositorios en
  `src/main/java/com/villo/truco/infrastructure/config/PresenceUseCaseConfiguration.java`.
- [X] T009 Cubrir el escenario "usuario libre → `busy=false` y refs en `null`" (FR-008/SC-004) en
  `GetUserPresenceQueryHandlerTest` y `PresenceControllerTest`. El `401` (FR-002) lo garantiza la
  config de seguridad existente (`/api/**` está `authenticated()`), igual que el resto de recursos.

**Checkpoint**: El endpoint existe, está protegido por JWT y responde "usuario libre".

---

## Phase 3: User Story 1 - Reconexión a una partida en curso (Priority: P1) 🎯 MVP

**Goal**: Que la presencia reporte la partida no finalizada del usuario (id + estado) para permitir
la reconexión a la partida en curso.

**Independent Test**: Con un usuario con una partida en estado no finalizado, consultar
`GET /api/me/presence` y verificar que `match` trae `id` y `status`; con un usuario sin partida,
`match` es `null`.

### Tests for User Story 1

- [X] T010 [US1] Crear `GetUserPresenceQueryHandlerTest` con casos: "con partida no finalizada
  expone match ref con id y estado" y "sin partida activa el match es nulo" (repos mockeados) en
  `src/test/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandlerTest.java`.

### Implementation for User Story 1

- [X] T011 [US1] Agregar `Optional<Match> findUnfinishedByPlayer(PlayerId playerId)` a la interfaz
  de dominio `src/main/java/com/villo/truco/domain/ports/MatchQueryRepository.java`.
- [X] T012 [US1] Agregar `@Query` `findUnfinishedByPlayer` (status NOT IN ('FINISHED','CANCELLED'),
  playerOne o playerTwo = :playerId, ORDER BY lastActivityAt DESC, limitado vía `Pageable`) a
  `src/main/java/com/villo/truco/infrastructure/persistence/repositories/spring/SpringDataMatchRepository.java`.
- [X] T013 [US1] Implementar `findUnfinishedByPlayer` en
  `src/main/java/com/villo/truco/infrastructure/persistence/repositories/JpaMatchRepositoryAdapter.java`
  (`PageRequest.of(0, 1)` + mapear primer resultado a `Match` de dominio).
- [X] T014 [P] [US1] Crear el record `ActiveMatchRefDTO(String id, String status)` en
  `src/main/java/com/villo/truco/application/dto/ActiveMatchRefDTO.java`.
- [X] T015 [US1] Resolver `match` en `GetUserPresenceQueryHandler`: `findUnfinishedByPlayer` →
  mapear a `ActiveMatchRefDTO` (id + status.name()) en
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T016 [P] [US1] Agregar sub-record `ActiveMatchRef` y su mapeo en `UserPresenceResponse.from`
  en `src/main/java/com/villo/truco/infrastructure/http/dto/response/UserPresenceResponse.java`.
- [X] T017 [US1] Cubrir "usuario con partida en curso → match ref con id/estado y busy=true" en
  `GetUserPresenceQueryHandlerTest` y `PresenceControllerTest`.

**Checkpoint**: MVP funcional — el FE puede reconectar a la partida en curso. US1 testeable sola.

---

## Phase 4: User Story 2 - Reconexión a una liga o copa (Priority: P2)

**Goal**: Que la presencia reporte la liga y/o copa del usuario (id + estado + `currentMatchId` del
torneo cuando esté en progreso).

**Independent Test**: Con un usuario en una liga en espera → `league` con id y estado de espera y
`currentMatchId` nulo; en progreso → `currentMatchId` igual a `match.id`. Análogo para copa.

### Tests for User Story 2

- [X] T018 [US2] Agregar a `GetUserPresenceQueryHandlerTest` casos para liga (en espera, en progreso
  con `currentMatchId == match.id`) y copa (en espera) en
  `src/test/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandlerTest.java`.

### Implementation for User Story 2

- [X] T019 [P] [US2] Crear el record `ActiveLeagueRefDTO(String id, String status,
  String currentMatchId)` en
  `src/main/java/com/villo/truco/application/dto/ActiveLeagueRefDTO.java`.
- [X] T020 [P] [US2] Crear el record `ActiveCupRefDTO(String id, String status,
  String currentMatchId)` en `src/main/java/com/villo/truco/application/dto/ActiveCupRefDTO.java`.
- [X] T021 [US2] Resolver `league` en `GetUserPresenceQueryHandler`: `findInProgressByPlayer` y, si
  vacío, `findWaitingByPlayer`; derivar `currentMatchId` de la partida no finalizada ya resuelta
  cuando la liga esté `IN_PROGRESS` (Decisión 4 de research) en
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T022 [US2] Resolver `cup` análogamente (`findInProgressByPlayer`/`findWaitingByPlayer` +
  `currentMatchId`) en el mismo
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T023 [P] [US2] Agregar sub-records `ActiveLeagueRef` y `ActiveCupRef` y su mapeo en
  `UserPresenceResponse.from` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/UserPresenceResponse.java`.
- [X] T024 [US2] Cubrir liga en progreso (`currentMatchId == match.id`) y copa en espera en
  `GetUserPresenceQueryHandlerTest`.

**Checkpoint**: US1 y US2 funcionan de forma independiente. Coherencia torneo↔partida verificada.

---

## Phase 5: User Story 3 - Retorno a una revancha pendiente (Priority: P3)

**Goal**: Que la presencia reporte la sesión de revancha abierta del usuario (id + `originMatchId`).

**Independent Test**: Con un usuario con una sesión de revancha abierta → `rematch` con `id` y
`originMatchId`; sin sesión → `rematch` nulo.

### Tests for User Story 3

- [X] T025 [US3] Agregar a `GetUserPresenceQueryHandlerTest` el caso "con sesión de revancha abierta
  expone rematch ref" en
  `src/test/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandlerTest.java`.

### Implementation for User Story 3

- [X] T026 [P] [US3] Crear el record `ActiveRematchRefDTO(String id, String originMatchId)` en
  `src/main/java/com/villo/truco/application/dto/ActiveRematchRefDTO.java`.
- [X] T027 [US3] Resolver `rematch` en `GetUserPresenceQueryHandler` vía
  `RematchSessionRepository.findOpenByPlayer` → mapear a `ActiveRematchRefDTO` en
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T028 [P] [US3] Agregar sub-record `ActiveRematchRef` y su mapeo en `UserPresenceResponse.from`
  en `src/main/java/com/villo/truco/infrastructure/http/dto/response/UserPresenceResponse.java`.
- [X] T029 [US3] Cubrir "usuario con revancha abierta → rematch ref" en
  `GetUserPresenceQueryHandlerTest`.

**Checkpoint**: Las tres historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentación y verificación final.

- [X] T030 [P] Actualizar `README.md`: listar el recurso REST nuevo `GET /api/me/presence` y la
  capacidad de presencia/reconexión.
- [X] T031 [P] Actualizar `docs/CONTRATOS_API.md`: agregar la sección del contrato de presencia
  (§7.6: request, respuesta de usuario ocupado y de usuario libre, códigos 200 y 401).
- [X] T032 Garantizar la solo-lectura (FR-009/SC-005): el handler usa solo métodos de consulta
  (`find*`), sin comandos, eventos ni `save`. Cubierto por el diseño y los tests unitarios del
  handler (no se invoca ninguna mutación).
- [ ] T033 Ejecutar `./gradlew build` (ArchUnit + coverage ≥70%) y la validación manual de
  `specs/008-user-presence/quickstart.md`. **PENDIENTE**: no pudo correrse en este entorno (Gradle
  no puede establecer la conexión loopback para forkear sus workers). Correr localmente.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA** todas las historias.
- **User Stories (Phase 3-5)**: dependen de Foundational. Comparten dos archivos
  (`GetUserPresenceQueryHandler.java` y `UserPresenceResponse.java`), por lo que sus tareas que los
  editan son **secuenciales entre historias**; el resto (sub-DTOs, tests, repo de match) es
  independiente.
- **Polish (Phase 6)**: depende de las historias deseadas completas.

### User Story Dependencies

- **US1 (P1)**: arranca tras Foundational. Sin dependencias de otras historias.
- **US2 (P2)**: arranca tras Foundational. Reutiliza `match` de US1 para derivar `currentMatchId`.
- **US3 (P3)**: arranca tras Foundational. Sin dependencias de otras historias.

### Within Each User Story

- Tests primero (deben fallar antes de implementar).
- Repo (puerto → Spring Data → adapter) antes del handler que lo usa.
- Sub-DTO antes de usarlo en el handler/response.
- Handler antes de la verificación de integración.

---

## Implementation Strategy

### MVP First (User Story 1)

1. Completar Phase 1 (Setup) + Phase 2 (Foundational).
2. Completar Phase 3 (US1: partida).
3. **PARAR y VALIDAR**: probar la reconexión a partida de forma independiente.
4. Desplegar/demostrar el MVP.

### Incremental Delivery

1. Setup + Foundational → endpoint "usuario libre" + 401.
2. US1 → reconexión a partida (MVP).
3. US2 → reconexión a liga/copa.
4. US3 → retorno a revancha.
5. Polish → docs (README, CONTRATOS_API) + verificación final.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes.
- `GetUserPresenceQueryHandler.java` y `UserPresenceResponse.java` se editan en varias historias →
  esas tareas NO son `[P]` entre sí.
- No se crean tablas ni migraciones; el único cambio de dominio es `findUnfinishedByPlayer`.
- Quick match queda fuera de alcance (FR-010): no se consulta `QuickMatchQueuePort`.
- VO creados en el query que viaja (`GetUserPresenceQuery` hace `PlayerId.of`), nunca en infra:
  el controller solo pasa `jwt.getSubject()` (String).
- Al agregar `findUnfinishedByPlayer` al puerto `MatchQueryRepository`, se actualizaron los 15 fakes
  anónimos de tests que lo implementan.
