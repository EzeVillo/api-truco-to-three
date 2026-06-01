---

description: "Lista de tareas para la feature Catálogo de Logros"
---

# Tasks: Catálogo de Logros

**Input**: Documentos de diseño en `specs/007-achievements-catalog/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/get-achievements.md

**Tests**: Incluidos. La constitución (Principio III) exige tests con coverage mínimo del 70%, y el
plan los lista explícitamente (handler, controller slice y contrato).

**Organization**: Tareas agrupadas por user story para implementación y testeo independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1, US2)
- Cada tarea incluye la ruta de archivo exacta

## Path Conventions

Proyecto único backend (Clean/Hexagonal + DDD). Código en `src/main/java/...`, tests en
`src/test/java/...`, documentación en `docs/` y `README.md`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificar el punto de partida. No se requieren dependencias ni configuración nuevas.

- [X] T001 Confirmar que `AchievementCode` existe y enumera los logros vigentes en `src/main/java/com/villo/truco/profile/domain/model/AchievementCode.java` (fuente de verdad del catálogo; no se modifica).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No hay prerequisitos bloqueantes transversales. El catálogo se deriva del enum ya
existente y no se introduce persistencia ni schema nuevo.

**Checkpoint**: Sin trabajo foundational. Se puede comenzar directamente con User Story 1.

---

## Phase 3: User Story 1 - Ver qué logros existen en el juego (Priority: P1) 🎯 MVP

**Goal**: Exponer `GET /api/achievements` que devuelve el catálogo completo de logros (sus códigos),
idéntico para todo jugador autenticado, derivado de `AchievementCode`.

**Independent Test**: Consultar `GET /api/achievements` con un usuario autenticado (incluso sin
logros desbloqueados) y verificar que devuelve exactamente los códigos de `AchievementCode`, sin
omisiones ni duplicados.

### Tests for User Story 1 ⚠️ (escribir primero, deben fallar antes de implementar)

- [X] T002 [P] [US1] Test del handler en `src/test/java/com/villo/truco/profile/application/usecases/queries/GetAchievementCatalogQueryHandlerTest.java`: verifica que el catálogo devuelto contiene exactamente `AchievementCode.values()`, en orden de declaración y sin duplicados.
- [X] T003 [P] [US1] Test de slice MVC del controller en `src/test/java/com/villo/truco/profile/infrastructure/http/AchievementControllerTest.java`: `GET /api/achievements` autenticado responde 200 con `achievements[].achievementCode` = todos los códigos; sin token responde 401; la respuesta no incluye campos de título/descripción/estado.
- [X] T004 [P] [US1] Test de contrato en `src/test/java/com/villo/truco/profile/infrastructure/http/AchievementCatalogContractTest.java`: parsea la sección §8.3 de `docs/CONTRATOS_API.md` y verifica que el conjunto de códigos coincide exactamente con `AchievementCode.values()` (FR-008).

### Implementation for User Story 1

- [X] T005 [P] [US1] Crear `AchievementCatalogDTO` en `src/main/java/com/villo/truco/profile/application/dto/AchievementCatalogDTO.java`: record con la lista de `AchievementCode` del catálogo.
- [X] T006 [P] [US1] Crear el puerto de entrada `GetAchievementCatalogUseCase` en `src/main/java/com/villo/truco/profile/application/usecases/queries/GetAchievementCatalogUseCase.java` (interface que devuelve `AchievementCatalogDTO`).
- [X] T007 [US1] Implementar `GetAchievementCatalogQueryHandler` en `src/main/java/com/villo/truco/profile/application/usecases/queries/GetAchievementCatalogQueryHandler.java`: devuelve `AchievementCatalogDTO` con `AchievementCode.values()` (depende de T005, T006).
- [X] T008 [P] [US1] Crear `AchievementCatalogResponse` (con su item `achievementCode`) en `src/main/java/com/villo/truco/profile/infrastructure/http/dto/response/AchievementCatalogResponse.java`, con método `from(AchievementCatalogDTO)` y anotaciones `@Schema` en español; el campo se llama `achievementCode` para coincidir con el perfil.
- [X] T009 [US1] Crear `AchievementController` en `src/main/java/com/villo/truco/profile/infrastructure/http/AchievementController.java`: `GET /api/achievements`, depende del puerto `GetAchievementCatalogUseCase`, anotaciones OpenAPI (`@Tag`, `@Operation`, `@ApiResponses` 200/401, `bearerAuth`) en español (depende de T006, T008).
- [X] T010 [US1] Cablear el bean del caso de uso en `src/main/java/com/villo/truco/profile/infrastructure/config/ProfileUseCaseConfiguration.java`, siguiendo el patrón existente de `getPlayerProfileUseCase` (depende de T007).
- [X] T011 [US1] Documentar el endpoint nuevo `GET /api/achievements` en `docs/CONTRATOS_API.md` (request sin body, response `AchievementCatalogResponse`, auth, 200/401) y dejar §8.3 como lista canónica de códigos (depende de T009).

**Checkpoint**: `GET /api/achievements` funcional y testeable de forma independiente. MVP entregable.

---

## Phase 4: User Story 2 - Ver mi progreso de logros (Priority: P2)

**Goal**: Permitir que el frontend cruce el catálogo (US1) con los logros desbloqueados del perfil
para mostrar todos los logros con marca de desbloqueado. El backend **no cambia su forma**: el
perfil ya devuelve solo desbloqueados con su detalle.

**Independent Test**: Para un jugador con algunos logros, cruzar la respuesta de
`GET /api/achievements` con `GET /api/profile/{username}` por `achievementCode` y verificar que cada
logro del catálogo queda marcado como desbloqueado o pendiente, y que los desbloqueados conservan
`unlockedAt`, `matchId` y `gameNumber`.

### Tests for User Story 2 ⚠️

- [X] T012 [P] [US2] Test de invariante del merge en `src/test/java/com/villo/truco/profile/infrastructure/http/AchievementCatalogProgressTest.java`: verifica que todo logro desbloqueable (`AchievementCode`) aparece en el catálogo, de modo que el cruce catálogo+perfil por `achievementCode` nunca deja un desbloqueado huérfano; y que catálogo y perfil comparten el nombre de campo `achievementCode`. (Se implementó como test de invariante liviano en lugar de un IT con Spring Boot, por YAGNI: el merge vive en el FE y un IT que desbloquee logros sería pesado y frágil.)

### Implementation for User Story 2

- [X] T013 [US2] Verificar (sin modificar la forma) que `GetPlayerProfileQueryHandler` y `PlayerProfileResponse` siguen devolviendo solo los logros desbloqueados con su detalle; si todo está correcto, no se cambia código de producción. Confirmado en `src/main/java/com/villo/truco/profile/application/usecases/queries/GetPlayerProfileQueryHandler.java` y `src/main/java/com/villo/truco/profile/infrastructure/http/dto/response/PlayerProfileResponse.java`.

**Checkpoint**: Catálogo + perfil permiten construir la grilla "todos con flag" en el FE; ambos
recursos funcionan de forma independiente.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Documentación y cierre.

- [X] T014 [P] Agregar `GET /api/achievements` (catálogo de logros) al listado de recursos REST / capacidades en `README.md`.
- [X] T015 [P] Corregir en `docs/CONTRATOS_API.md` el ejemplo obsoleto del perfil (~línea 1646) que usa `WIN_RETRUCO_FROM_0_0_TO_3` (código inexistente) por un código real de `AchievementCode`.
- [ ] T016 Ejecutar `./gradlew build` y validar suite completa, ArchUnit y coverage ≥ 70%; luego correr la validación manual de `specs/007-achievements-catalog/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: vacía; no bloquea.
- **User Story 1 (Phase 3)**: MVP. Sin dependencias de otras stories.
- **User Story 2 (Phase 4)**: depende de US1 para que exista el catálogo a cruzar (la spec lo
  reconoce). No requiere código de producción nuevo.
- **Polish (Phase 5)**: tras completar las stories deseadas.

### User Story Dependencies

- **US1 (P1)**: independiente. Entrega el MVP.
- **US2 (P2)**: usa el endpoint de US1 + el perfil existente; el merge es responsabilidad del FE.

### Within User Story 1

- Tests (T002–T004) primero y deben fallar.
- DTO/puerto (T005, T006) → handler (T007) → response DTO (T008) → controller (T009) → wiring
  (T010) → doc (T011).

### Parallel Opportunities

- T002, T003, T004 (tests de US1) en paralelo.
- T005, T006, T008 (DTO, puerto, response DTO) en paralelo entre sí.
- T014 y T015 (docs de polish) en paralelo.

---

## Parallel Example: User Story 1

```bash
# Tests de US1 (escribir y ver que fallen) en paralelo:
Task: "Test del handler en GetAchievementCatalogQueryHandlerTest.java"
Task: "Test del controller en AchievementControllerTest.java"
Task: "Test de contrato en AchievementCatalogContractTest.java"

# Bloques de implementación independientes en paralelo:
Task: "Crear AchievementCatalogDTO en application/dto/AchievementCatalogDTO.java"
Task: "Crear puerto GetAchievementCatalogUseCase en application/usecases/queries/"
Task: "Crear AchievementCatalogResponse en infrastructure/http/dto/response/"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (Setup) → Phase 3 (US1 completa).
2. **STOP y validar**: `GET /api/achievements` devuelve el catálogo completo.
3. Entregable: el FE ya puede dejar de hardcodear la lista de logros.

### Incremental Delivery

1. US1 → catálogo funcionando (MVP).
2. US2 → el FE arma la grilla con marca de desbloqueado cruzando catálogo + perfil.
3. Polish → docs y verificación final.

---

## Notes

- [P] = archivos distintos, sin dependencias entre sí.
- Verificar que los tests fallen antes de implementar.
- Commit después de cada tarea o grupo lógico.
- US2 no agrega código de producción: si la verificación T013 detecta un desvío en la forma del
  perfil, eso sería un bug aparte, no parte de esta feature.
