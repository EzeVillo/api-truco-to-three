---
description: "Lista de tareas para la feature 010-friend-spectate"
---

# Tareas: Espectar partidas de amigos

**Input**: Documentos de diseño en `specs/010-friend-spectate/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/friend-spectate.md](contracts/friend-spectate.md),
[quickstart.md](quickstart.md)

**Tests**: Obligatorios por Constitution. Escribir primero los tests de cada historia y verificar
que fallen antes de implementar.

**Organization**: Tareas agrupadas por historia de usuario para permitir implementación y prueba
independiente.

## Formato: `[ID] [P?] [Story] Descripción`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencia de tareas incompletas).
- **[Story]**: Historia de usuario asociada (`US1`, `US2`, `US3`).
- Todas las tareas incluyen paths concretos.

## Phase 1: Setup

**Propósito**: Preparar la base documental y ubicar los puntos de extensión existentes antes de
tocar comportamiento.

- [X] T001 Revisar y confirmar contra `docs/CONTRATOS_API.md` las secciones de spectate,
  `GET /api/social/friends` y eventos `/user/queue/match-spectate`.
- [X] T002 [P] Revisar el flujo actual de spectate en
  `src/main/java/com/villo/truco/infrastructure/websocket/SpectateSubscribeEventListener.java`.
- [X] T003 [P] Revisar la política actual de spectate en
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicy.java`.
- [X] T004 [P] Revisar consultas de amistad aceptada en
  `src/main/java/com/villo/truco/social/domain/ports/FriendshipQueryRepository.java`.

---

## Phase 2: Foundational

**Propósito**: Crear los contratos internos compartidos que bloquean todas las historias.

**CRÍTICO**: Ninguna historia debe implementarse hasta completar esta fase.

- [X] T005 Crear el puerto `FriendshipSpectateEligibilityResolver` en
  `src/main/java/com/villo/truco/domain/ports/FriendshipSpectateEligibilityResolver.java`.
- [X] T006 [P] Crear el DTO `SpectatableMatchRefDTO` en
  `src/main/java/com/villo/truco/social/application/dto/SpectatableMatchRefDTO.java`.
- [X] T007 [P] Agregar el reason `FRIENDSHIP_REMOVED` en
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatorshipStopReason.java`.
- [X] T008 Modificar el constructor de `SpectatingEligibilityPolicy` para recibir
  `FriendshipSpectateEligibilityResolver` en
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicy.java`.
- [X] T009 Crear el adapter `SocialFriendshipSpectateEligibilitySupport` en
  `src/main/java/com/villo/truco/social/application/services/SocialFriendshipSpectateEligibilitySupport.java`.
- [X] T010 Registrar el bean `SocialFriendshipSpectateEligibilitySupport` en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialUseCaseConfiguration.java`.
- [X] T011 Inyectar `FriendshipSpectateEligibilityResolver` en `SpectatorConfiguration` y pasarlo a
  `SpectatingEligibilityPolicy` en
  `src/main/java/com/villo/truco/infrastructure/config/SpectatorConfiguration.java`.

**Checkpoint**: La política puede consultar amistad aceptada sin acoplar dominio a social.

---

## Phase 3: User Story 1 - Entrar a espectar una partida activa de un amigo (Priority: P1) MVP

**Goal**: Un usuario ve que un amigo está jugando, obtiene el `matchId` desde la lista social y
entra
como espectador usando el flujo WebSocket existente.

**Independent Test**: Con dos usuarios amigos, uno en match `IN_PROGRESS`, `GET /api/social/friends`
devuelve `spectatableMatch` y la suscripción a `/user/queue/match-spectate` responde
`SPECTATE_STATE`.

### Tests para User Story 1

- [X] T012 [P] [US1] Agregar test de política que permite espectar por amistad aceptada en
  `src/test/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicyTest.java`.
- [X] T013 [P] [US1] Agregar test de handler que registra spectatorship y publica conteo para amigo
  confirmado en
  `src/test/java/com/villo/truco/application/usecases/commands/SpectateMatchCommandHandlerTest.java`.
- [X] T014 [P] [US1] Crear test de lista de amigos con `spectatableMatch` para amigo en match
  `IN_PROGRESS` en
  `src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandlerTest.java`.
- [X] T015 [P] [US1] Ampliar test HTTP para serializar `spectatableMatch` en
  `src/test/java/com/villo/truco/social/infrastructure/http/FriendshipControllerTest.java`.

### Implementación para User Story 1

- [X] T016 [US1] Actualizar `SpectatingEligibilityPolicy.ensureCanStartWatching` para aceptar
  competencia o amistad confirmada en
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicy.java`.
- [X] T017 [US1] Implementar `SocialFriendshipSpectateEligibilitySupport` consultando
  `existsAcceptedByPlayers` para ambos jugadores del match en
  `src/main/java/com/villo/truco/social/application/services/SocialFriendshipSpectateEligibilitySupport.java`.
- [X] T018 [US1] Agregar `spectatableMatch` nullable a `FriendSummaryDTO` en
  `src/main/java/com/villo/truco/social/application/dto/FriendSummaryDTO.java`.
- [X] T019 [US1] Modificar `SocialViewAssembler.toFriendSummaryDto` para recibir y mapear
  `SpectatableMatchRefDTO` en
  `src/main/java/com/villo/truco/social/application/services/SocialViewAssembler.java`.
- [X] T020 [US1] Modificar `GetFriendsQueryHandler` para resolver el amigo contraparte y consultar
  `MatchQueryRepository.findUnfinishedByPlayer` en
  `src/main/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandler.java`.
- [X] T021 [US1] Asegurar que `GetFriendsQueryHandler` solo asigne `spectatableMatch` cuando el
  match esté `IN_PROGRESS` en
  `src/main/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandler.java`.
- [X] T022 [US1] Agregar sub-record `SpectatableMatchRefResponse` y mapeo desde DTO en
  `src/main/java/com/villo/truco/social/infrastructure/http/dto/response/FriendSummaryResponse.java`.
- [X] T023 [US1] Inyectar `MatchQueryRepository` en la construcción de `GetFriendsQueryHandler` en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialUseCaseConfiguration.java`.
- [X] T024 [US1] Verificar que `SpectateSubscribeEventListener` no requiere cambios de contrato y
  documentar con test existente en
  `src/test/java/com/villo/truco/infrastructure/websocket/SpectateSubscribeEventListenerTest.java`.

**Checkpoint**: US1 funciona de punta a punta como MVP.

---

## Phase 4: User Story 2 - Restringir el acceso a amigos confirmados (Priority: P2)

**Goal**: Usuarios sin amistad aceptada, con amistad pendiente/removida o intentando mirar partida
propia no pueden espectar.

**Independent Test**: Un usuario sin amistad confirmada intenta suscribirse al match y recibe
`SPECTATE_ERROR`; la lista social no expone `spectatableMatch` fuera de amigos confirmados.

### Tests para User Story 2

- [X] T025 [P] [US2] Agregar tests de política para rechazar amistad pendiente, removida o
  inexistente en
  `src/test/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicyTest.java`.
- [X] T026 [P] [US2] Agregar test de comando que rechaza usuario sin amistad ni competencia en
  `src/test/java/com/villo/truco/application/usecases/commands/SpectateMatchCommandHandlerTest.java`.
- [X] T027 [P] [US2] Agregar test de WebSocket que traduce rechazo por elegibilidad en
  `SPECTATE_ERROR` en
  `src/test/java/com/villo/truco/infrastructure/websocket/SpectateSubscribeEventListenerTest.java`.
- [X] T028 [P] [US2] Agregar test de lista de amigos que mantiene `spectatableMatch: null` si el
  amigo no está en match `IN_PROGRESS` en
  `src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandlerTest.java`.

### Implementación para User Story 2

- [X] T029 [US2] Ajustar `SpectateNotAllowedException` para describir liga/copa o amistad confirmada
  en
  `src/main/java/com/villo/truco/domain/model/spectator/exceptions/SpectateNotAllowedException.java`.
- [X] T030 [US2] Asegurar que `SpectatingEligibilityPolicy` evalúa amistad solo después de bloquear
  partida propia en
  `src/main/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicy.java`.
- [X] T031 [US2] Asegurar que `SocialFriendshipSpectateEligibilitySupport` devuelve `false` para
  `playerTwo == null` y no consulta amistades inválidas en
  `src/main/java/com/villo/truco/social/application/services/SocialFriendshipSpectateEligibilitySupport.java`.
- [X] T032 [US2] Ajustar `FriendSummaryResponse.from` para serializar `spectatableMatch` como `null`
  cuando no haya partida espectable en
  `src/main/java/com/villo/truco/social/infrastructure/http/dto/response/FriendSummaryResponse.java`.

**Checkpoint**: US1 y US2 quedan funcionales; el acceso queda limitado a amigos confirmados o modos
existentes.

---

## Phase 5: User Story 3 - Mantener la experiencia correcta durante y al finalizar la partida (Priority: P3)

**Goal**: La sesión de espectador conserva reconexión, snapshot, conteo y cleanup existentes; además
se invalida cuando una amistad removida era el único motivo de acceso.

**Independent Test**: Un amigo entra como espectador, la amistad se elimina, se corta la sesión si
no
queda liga/copa y los reintentos fallan; si también pertenece a liga/copa, la sesión se conserva.

### Tests para User Story 3

- [X] T033 [P] [US3] Crear test de cleanup por amistad removida que corta spectate habilitado solo
  por amistad en
  `src/test/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandlerTest.java`.
- [X] T034 [P] [US3] Agregar test de cleanup que conserva spectate si el usuario sigue habilitado
  por liga/copa en
  `src/test/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandlerTest.java`.
- [X] T035 [P] [US3] Agregar test de lifecycle para publicar conteo al cortar por
  `FRIENDSHIP_REMOVED` en
  `src/test/java/com/villo/truco/application/usecases/commands/StopSpectatingMatchCommandHandlerTest.java`.
- [X] T036 [P] [US3] Agregar test de quickstart para snapshot REST posterior a alta válida en
  `src/test/java/com/villo/truco/application/usecases/queries/GetSpectateMatchStateQueryHandlerTest.java`.

### Implementación para User Story 3

- [X] T037 [US3] Extender `SpectatorshipLifecycleManager.forceStop` para aceptar y persistir reason
  `FRIENDSHIP_REMOVED` sin romper reasons existentes en
  `src/main/java/com/villo/truco/application/usecases/commands/SpectatorshipLifecycleManager.java`.
- [X] T038 [US3] Crear `SpectatorCleanupOnFriendshipRemovedEventHandler` que maneje
  `FriendshipRemovedEvent` en
  `src/main/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandler.java`.
- [X] T039 [US3] Implementar en el handler la búsqueda de spectatorships activos de
  requester/addressee usando `SpectatorshipRepository.findBySpectatorId` en
  `src/main/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandler.java`.
- [X] T040 [US3] Implementar en el handler la reevaluación de elegibilidad restante con
  `MatchQueryRepository`, `CompetitionMembershipResolver` y `FriendshipSpectateEligibilityResolver`
  en
  `src/main/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandler.java`.
- [X] T041 [US3] Registrar `SpectatorCleanupOnFriendshipRemovedEventHandler` como bean en
  `src/main/java/com/villo/truco/infrastructure/config/SpectatorConfiguration.java`.
- [X] T042 [US3] Agregar el handler de cleanup a `CompositeSocialEventNotifier` en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialApplicationEventConfiguration.java`.
- [X] T043 [US3] Confirmar que `SpectatorNotificationEventTranslator` sigue filtrando eventos
  privados por asiento para amigos espectadores en
  `src/main/java/com/villo/truco/application/eventhandlers/SpectatorNotificationEventTranslator.java`.

**Checkpoint**: Todas las historias quedan funcionales y el ciclo de vida de spectate sigue alineado
al flujo existente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Propósito**: Documentación, contrato y verificación completa.

- [X] T044 [P] Actualizar `docs/CONTRATOS_API.md` con elegibilidad por amistad, `spectatableMatch` y
  nota de alta WebSocket-first en `docs/CONTRATOS_API.md`.
- [X] T045 [P] Actualizar `README.md` con la capacidad de espectar amigos y el flujo
  `/user/queue/match-spectate` en `README.md`.
- [X] T046 [P] Actualizar ejemplos o utilidades manuales de WebSocket si corresponde en
  `src/main/resources/WebSocketTest.html`.
- [X] T047 Ejecutar `.\gradlew.bat test` desde
  `C:\Users\ezevi_30ey2ks\Desktop\Proyectos\api-truco-to-three` y corregir regresiones.
- [X] T048 Validar manualmente los escenarios de `specs/010-friend-spectate/quickstart.md`.
- [X] T049 Revisar `docs/CONTRATOS_API.md` contra los DTOs finales `FriendSummaryResponse` y
  `SpectatorMatchStateResponse` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/`.

---

## Dependencias y Orden de Ejecución

### Dependencias por fase

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Phase 1 y bloquea todas las historias.
- **US1 (Phase 3)**: depende de Foundational. Es el MVP.
- **US2 (Phase 4)**: depende de Foundational y puede avanzar en paralelo con US1 después de que
  exista la política base.
- **US3 (Phase 5)**: depende de Foundational; para implementación completa conviene tener US1/US2.
- **Polish (Phase 6)**: depende de las historias implementadas.

### Dependencias por historia

- **US1**: habilita el flujo feliz y entrega MVP.
- **US2**: endurece reglas negativas y acceso denegado.
- **US3**: completa ciclo de vida e invalidación por amistad removida.

### Dentro de cada historia

- Tests antes de implementación.
- DTOs/puertos antes de handlers.
- Política antes de wiring.
- Wiring antes de pruebas HTTP/WebSocket completas.

## Oportunidades de Paralelización

- T002, T003 y T004 pueden hacerse en paralelo.
- T006 y T007 pueden hacerse en paralelo después de T005.
- T012, T013, T014 y T015 pueden escribirse en paralelo.
- T025, T026, T027 y T028 pueden escribirse en paralelo.
- T033, T034, T035 y T036 pueden escribirse en paralelo.
- T044, T045 y T046 pueden hacerse en paralelo al cerrar implementación.

## Ejemplos de Paralelo

### User Story 1

```text
Task: "T012 [P] [US1] Agregar test de política que permite espectar por amistad aceptada en src/test/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicyTest.java"
Task: "T014 [P] [US1] Crear test de lista de amigos con spectatableMatch para amigo en match IN_PROGRESS en src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandlerTest.java"
Task: "T015 [P] [US1] Ampliar test HTTP para serializar spectatableMatch en src/test/java/com/villo/truco/social/infrastructure/http/FriendshipControllerTest.java"
```

### User Story 2

```text
Task: "T025 [P] [US2] Agregar tests de política para rechazar amistad pendiente, removida o inexistente en src/test/java/com/villo/truco/domain/model/spectator/SpectatingEligibilityPolicyTest.java"
Task: "T027 [P] [US2] Agregar test de WebSocket que traduce rechazo por elegibilidad en SPECTATE_ERROR en src/test/java/com/villo/truco/infrastructure/websocket/SpectateSubscribeEventListenerTest.java"
```

### User Story 3

```text
Task: "T033 [P] [US3] Crear test de cleanup por amistad removida que corta spectate habilitado solo por amistad en src/test/java/com/villo/truco/application/eventhandlers/SpectatorCleanupOnFriendshipRemovedEventHandlerTest.java"
Task: "T036 [P] [US3] Agregar test de quickstart para snapshot REST posterior a alta válida en src/test/java/com/villo/truco/application/usecases/queries/GetSpectateMatchStateQueryHandlerTest.java"
```

## Estrategia de Implementación

### MVP primero

1. Completar Phase 1 y Phase 2.
2. Completar US1.
3. Ejecutar tests relacionados con política, comando, amigos y controller social.
4. Validar el flujo feliz del quickstart.

### Entrega incremental

1. US1: amigo ve `spectatableMatch` y entra por spectate existente.
2. US2: se bloquean usuarios sin amistad confirmada y estados no válidos.
3. US3: se completa invalidación por amistad removida y lifecycle.
4. Polish: docs, contrato y suite completa.

### Criterios de cierre

- `.\gradlew.bat test` pasa.
- `docs/CONTRATOS_API.md` refleja el contrato final.
- `README.md` menciona la capacidad social.
- No hay endpoint REST nuevo de alta de spectate.
- No existe opt-out para espectadores amigos.

