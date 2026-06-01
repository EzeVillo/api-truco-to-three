# Tasks: Identidad de sesion auth

**Input**: Design documents from `specs/006-auth-session-identity/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/auth-session-identity.md](contracts/auth-session-identity.md)

**Tests**: Requeridos por constitution. Escribir/ajustar tests antes de implementar cada slice.

**Organization**: Tareas agrupadas por user story para permitir implementacion y verificacion
independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencia de tareas incompletas).
- **[Story]**: User story asociada (`US1`, `US2`, `US3`).
- Cada tarea incluye paths concretos.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar contexto activo y preparar la feature sin tocar comportamiento.

- [X] T001 Verificar que `.specify/feature.json` apunte a `specs/006-auth-session-identity`
- [X] T002 Revisar contratos de auth actuales en
  `src/main/java/com/villo/truco/auth/infrastructure/http/dto/response/`
- [X] T003 [P] Revisar tests actuales de auth en `src/test/java/com/villo/truco/auth/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear las piezas compartidas que bloquean US1 y US2.

**CRITICAL**: No empezar user stories hasta completar esta fase.

- [X] T004 Agregar lookup por playerId individual al puerto
  `src/main/java/com/villo/truco/auth/domain/ports/UserQueryRepository.java`
- [X] T005 Implementar lookup por playerId individual en
  `src/main/java/com/villo/truco/auth/infrastructure/persistence/repositories/JpaUserQueryRepositoryAdapter.java`
- [X] T006 [P] Agregar cobertura del lookup por playerId en
  `src/test/java/com/villo/truco/auth/infrastructure/persistence/repositories/JpaUserQueryRepositoryAdapterTest.java`
- [X] T007 Crear modelo de lectura `AuthenticatedSessionIdentity` en
  `src/main/java/com/villo/truco/auth/application/model/AuthenticatedSessionIdentity.java`
- [X] T008 Crear query `GetCurrentSessionIdentityQuery` en
  `src/main/java/com/villo/truco/auth/application/queries/GetCurrentSessionIdentityQuery.java`
- [X] T009 Crear puerto de entrada `GetCurrentSessionIdentityUseCase` en
  `src/main/java/com/villo/truco/auth/application/ports/in/GetCurrentSessionIdentityUseCase.java`

**Checkpoint**: La base compartida existe, sin endpoint publico todavia.

---

## Phase 3: User Story 1 - Rehidratar usuario autenticado tras recargar (Priority: P1) MVP

**Goal**: Permitir que el FE recupere `playerId`, `username` y `tokenUse` con un access token
valido,
sin username guardado localmente.

**Independent Test**: Conservar solo un access token valido, llamar `GET /api/auth/me` y verificar
que la respuesta devuelva identidad de usuario registrado; con token guest debe devolver
`username = null`.

### Tests for User Story 1

- [X] T010 [P] [US1] Crear tests del caso de uso de identidad actual en
  `src/test/java/com/villo/truco/auth/application/usecases/queries/GetCurrentSessionIdentityQueryHandlerTest.java`
- [X] T011 [P] [US1] Ampliar tests de controller para `GET /api/auth/me` registrado y guest en
  `src/test/java/com/villo/truco/auth/infrastructure/http/AuthControllerTest.java`
- [X] T012 [P] [US1] Agregar test de seguridad HTTP para `GET /api/auth/me` sin token en
  `src/test/java/com/villo/truco/infrastructure/security/HttpSecurityIntegrationTest.java`

### Implementation for User Story 1

- [X] T013 [US1] Implementar `GetCurrentSessionIdentityQueryHandler` en
  `src/main/java/com/villo/truco/auth/application/usecases/queries/GetCurrentSessionIdentityQueryHandler.java`
- [X] T014 [US1] Registrar `GetCurrentSessionIdentityUseCase` en
  `src/main/java/com/villo/truco/auth/infrastructure/config/AuthUseCaseConfiguration.java`
- [X] T015 [US1] Crear DTO `CurrentSessionResponse` en
  `src/main/java/com/villo/truco/auth/infrastructure/http/dto/response/CurrentSessionResponse.java`
- [X] T016 [US1] Inyectar el caso de uso y exponer `GET /api/auth/me` en
  `src/main/java/com/villo/truco/auth/infrastructure/http/AuthController.java`
- [X] T017 [US1] Verificar que `GET /api/auth/me` use `Jwt.getSubject()` y claim `token_use` sin
  modificar `src/main/java/com/villo/truco/auth/infrastructure/security/JwtAccessTokenProvider.java`

**Checkpoint**: US1 funcional y testeable de forma independiente.

---

## Phase 4: User Story 2 - Recibir username durante el ciclo de auth (Priority: P2)

**Goal**: Incluir `username` en register, login y refresh para usuarios registrados.

**Independent Test**: Registrar, loguear y refrescar una sesion; cada respuesta debe incluir el
username autoritativo junto con tokens y expiraciones.

### Tests for User Story 2

- [X] T018 [P] [US2] Actualizar tests de register para esperar username en
  `src/test/java/com/villo/truco/auth/application/usecases/commands/RegisterUserCommandHandlerTest.java`
- [X] T019 [P] [US2] Actualizar tests de login para esperar username en
  `src/test/java/com/villo/truco/auth/application/usecases/commands/LoginCommandHandlerTest.java`
- [X] T020 [P] [US2] Actualizar tests de refresh para esperar username en
  `src/test/java/com/villo/truco/auth/application/usecases/commands/RefreshUserSessionCommandHandlerTest.java`
- [X] T021 [P] [US2] Actualizar tests de controller para validar username en register/login/refresh
  en `src/test/java/com/villo/truco/auth/infrastructure/http/AuthControllerTest.java`

### Implementation for User Story 2

- [X] T022 [US2] Agregar `username` a `UserAuthenticatedSession` en
  `src/main/java/com/villo/truco/auth/application/model/UserAuthenticatedSession.java`
- [X] T023 [US2] Ajustar `UserSessionIssuer` para emitir sesiones registradas con username en
  `src/main/java/com/villo/truco/auth/application/services/UserSessionIssuer.java`
- [X] T024 [US2] Ajustar `RegisterUserCommandHandler` para pasar username al issuer en
  `src/main/java/com/villo/truco/auth/application/usecases/commands/RegisterUserCommandHandler.java`
- [X] T025 [US2] Ajustar `LoginCommandHandler` para pasar username al issuer en
  `src/main/java/com/villo/truco/auth/application/usecases/commands/LoginCommandHandler.java`
- [X] T026 [US2] Ajustar `RefreshUserSessionCommandHandler` para resolver username durante refresh
  en
  `src/main/java/com/villo/truco/auth/application/usecases/commands/RefreshUserSessionCommandHandler.java`
- [X] T027 [US2] Agregar campo `username` a `RegisterUserResponse` en
  `src/main/java/com/villo/truco/auth/infrastructure/http/dto/response/RegisterUserResponse.java`
- [X] T028 [US2] Agregar campo `username` a `LoginResponse` en
  `src/main/java/com/villo/truco/auth/infrastructure/http/dto/response/LoginResponse.java`
- [X] T029 [US2] Agregar campo `username` a `RefreshUserSessionResponse` en
  `src/main/java/com/villo/truco/auth/infrastructure/http/dto/response/RefreshUserSessionResponse.java`

**Checkpoint**: US1 y US2 funcionan de forma independiente y no cambian respuesta guest.

---

## Phase 5: User Story 3 - Mantener contratos publicos alineados (Priority: P3)

**Goal**: Alinear `CONTRATOS_API.md` y `README.md` con la nueva identidad de auth y con el
comportamiento real de profile.

**Independent Test**: Leer la documentacion y poder identificar como rehidratar username, que el JWT
no contiene username y que profile usa `GET /api/profile/{username}` con el shape real.

### Implementation for User Story 3

- [X] T030 [US3] Actualizar ejemplos de register/login/refresh en `docs/CONTRATOS_API.md`
- [X] T031 [US3] Documentar `GET /api/auth/me` en `docs/CONTRATOS_API.md`
- [X] T032 [US3] Actualizar flujo recomendado de autenticacion y notas FE en `docs/CONTRATOS_API.md`
- [X] T033 [US3] Corregir documentacion de profile case-insensitive y shape real en
  `docs/CONTRATOS_API.md`
- [X] T034 [US3] Actualizar secciones Auth y Profile en `README.md`

**Checkpoint**: La documentacion publica queda alineada con el contrato implementado.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validacion final, limpieza y cierre.

- [X] T035 [P] Revisar que no se haya agregado `username` al JWT en
  `src/main/java/com/villo/truco/auth/infrastructure/security/JwtAccessTokenProvider.java`
- [X] T036 [P] Revisar que no haya imports de Spring en application/domain bajo
  `src/main/java/com/villo/truco/auth/`
- [X] T037 Ejecutar suite completa con `.\gradlew.bat test`
- [X] T038 Revisar quickstart manual en `specs/006-auth-session-identity/quickstart.md`
- [X] T039 Revisar `git diff` para asegurar que no se tocaron cambios no relacionados

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; bloquea US1 y US2.
- **US1 (Phase 3)**: depende de Foundational; MVP.
- **US2 (Phase 4)**: depende de Foundational; puede implementarse despues o en paralelo con US1 si
  se coordinan cambios en `AuthControllerTest.java`.
- **US3 (Phase 5)**: depende del contrato decidido; conviene hacerla despues de US1+US2 para
  documentar shapes finales.
- **Polish (Phase 6)**: depende de las historias que se quieran cerrar.

### User Story Dependencies

- **US1 (P1)**: no depende de US2 ni US3.
- **US2 (P2)**: no depende funcionalmente de US1, pero comparte modelos y tests de auth.
- **US3 (P3)**: depende de que US1 y US2 definan el contrato final.

### Within Each User Story

- Tests antes de implementacion.
- Modelos/puertos antes de handlers.
- Handlers antes de controller/DTO.
- Documentacion despues de confirmar shapes finales.

### Parallel Opportunities

- T003 puede hacerse en paralelo con T002.
- T006, T007, T008 y T009 tocan archivos distintos, pero T006 requiere el contrato de T004.
- T010, T011 y T012 pueden escribirse en paralelo.
- T018, T019, T020 y T021 pueden escribirse en paralelo, salvo coordinación sobre
  `AuthControllerTest.java`.
- T030 a T034 pueden repartirse cuando el contrato final este confirmado.
- T035 y T036 pueden ejecutarse en paralelo antes de correr la suite completa.

---

## Parallel Example: User Story 1

```text
Task: "Crear tests del caso de uso de identidad actual en src/test/java/com/villo/truco/auth/application/usecases/queries/GetCurrentSessionIdentityQueryHandlerTest.java"
Task: "Ampliar tests de controller para GET /api/auth/me registrado y guest en src/test/java/com/villo/truco/auth/infrastructure/http/AuthControllerTest.java"
Task: "Agregar test de seguridad HTTP para GET /api/auth/me sin token en src/test/java/com/villo/truco/infrastructure/security/HttpSecurityIntegrationTest.java"
```

---

## Parallel Example: User Story 2

```text
Task: "Actualizar tests de register para esperar username en src/test/java/com/villo/truco/auth/application/usecases/commands/RegisterUserCommandHandlerTest.java"
Task: "Actualizar tests de login para esperar username en src/test/java/com/villo/truco/auth/application/usecases/commands/LoginCommandHandlerTest.java"
Task: "Actualizar tests de refresh para esperar username en src/test/java/com/villo/truco/auth/application/usecases/commands/RefreshUserSessionCommandHandlerTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 y Phase 2.
2. Implementar US1 con tests primero.
3. Validar `GET /api/auth/me` para usuario registrado y guest.
4. Recién despues avanzar con username en register/login/refresh.

### Incremental Delivery

1. Foundation lista.
2. US1: endpoint de rehidratacion.
3. US2: username en lifecycle de auth.
4. US3: docs alineadas.
5. Polish: suite completa y revision de diff.

### Validation

La validacion final obligatoria es:

```powershell
.\gradlew.bat test
```

No ejecutar tests por clase.
