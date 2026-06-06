# Tasks: Actividad en vivo de amigos

**Input**: Design documents from `specs/011-friend-activity-push/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/friend-activity-push.md](contracts/friend-activity-push.md)

**Tests**: Obligatorios por constitucion del proyecto. Escribir primero los tests de cada historia y
verificar que fallan antes de implementar.

**Organization**: Tareas agrupadas por historia de usuario para permitir implementacion y validacion
independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo con otras tareas de la misma fase porque toca archivos
  distintos y no depende de una tarea incompleta.
- **[Story]**: Historia asociada (`US1`, `US2`, `US3`).
- Todas las tareas incluyen rutas exactas.

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: Preparar contratos, fixtures y puntos de extension compartidos.

- [x] T001 Revisar eventos sociales y de match existentes en
  `src/main/java/com/villo/truco/social/application/events/SocialEventNotification.java` y
  `src/main/java/com/villo/truco/domain/model/match/events/MatchDomainEvent.java`
- [x] T002 [P] Crear fixtures de actividad social para tests en
  `src/test/java/com/villo/truco/social/application/services/FriendActivityTestFixtures.java`
- [x] T003 [P] Crear helpers de captura de eventos de aplicacion para tests en
  `src/test/java/com/villo/truco/application/eventhandlers/FriendActivityEventCapture.java`

---

## Phase 2: Foundational (Prerequisitos bloqueantes)

**Purpose**: Modelos y puertos compartidos que bloquean las historias.

**CRITICAL**: Ninguna historia debe implementarse hasta completar esta fase.

- [x] T004 Crear DTO `FriendActivityDTO` con `friendUsername` y `spectatableMatch` en
  `src/main/java/com/villo/truco/social/application/dto/FriendActivityDTO.java`
- [x] T005 Crear DTO `FriendActivityStateDTO` con lista de amigos en
  `src/main/java/com/villo/truco/social/application/dto/FriendActivityStateDTO.java`
- [x] T006 Crear evento de aplicacion `FriendActivityNotification` para snapshot/delta social en
  `src/main/java/com/villo/truco/social/application/events/FriendActivityNotification.java`
- [x] T007 Crear query `GetFriendActivityQuery` en
  `src/main/java/com/villo/truco/social/application/queries/GetFriendActivityQuery.java`
- [x] T008 Crear puerto de entrada `GetFriendActivityUseCase` en
  `src/main/java/com/villo/truco/social/application/ports/in/GetFriendActivityUseCase.java`
- [x] T009 Extender `FriendshipQueryRepository` solo si hace falta una consulta mas eficiente de
  amistades aceptadas por participante en
  `src/main/java/com/villo/truco/social/domain/ports/FriendshipQueryRepository.java`

**Checkpoint**: Modelos base listos para implementar historias.

---

## Phase 3: User Story 1 - Ver amigos jugando en tiempo real (Priority: P1) MVP

**Goal**: Publicar deltas cuando un amigo confirmado empieza o deja de tener una partida espectable.

**Independent Test**: Con dos amigos confirmados, uno inicia y termina una partida; el otro recibe
`FRIEND_ACTIVITY_CHANGED` con `spectatableMatch` y luego con `null`, sin recargar.

### Tests for User Story 1

- [x] T010 [P] [US1] Crear test de resolucion de actividad con match `IN_PROGRESS` y match
  finalizado en
  `src/test/java/com/villo/truco/social/application/services/FriendActivityResolverTest.java`
- [x] T011 [P] [US1] Crear test de traduccion de eventos de match a `FRIEND_ACTIVITY_CHANGED` en
  `src/test/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslatorTest.java`
- [x] T012 [P] [US1] Crear test de publicacion STOMP de `FriendActivityNotification` en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/StompFriendActivityNotificationHandlerTest.java`

### Implementation for User Story 1

- [x] T013 [US1] Implementar `FriendActivityResolver` para calcular `FriendActivityDTO` desde
  amistad aceptada y match activo en
  `src/main/java/com/villo/truco/social/application/services/FriendActivityResolver.java`
- [x] T014 [US1] Implementar traduccion de `GameStartedEvent`, `MatchFinishedEvent`,
  `MatchCancelledEvent`, `MatchAbandonedEvent` y `MatchForfeitedEvent` a actividad social en
  `src/main/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslator.java`
- [x] T015 [US1] Agregar handler STOMP de `FriendActivityNotification` sin romper
  `SocialEventNotification` en
  `src/main/java/com/villo/truco/social/infrastructure/websocket/StompFriendActivityNotificationHandler.java`
- [x] T016 [US1] Registrar `FriendActivityMatchEventTranslator` y el handler STOMP correspondiente
  en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialApplicationEventConfiguration.java`
- [x] T017 [US1] Ejecutar tests de US1 con
  `.\gradlew.bat test --tests "*FriendActivityResolverTest" --tests "*FriendActivityMatchEventTranslatorTest" --tests "*StompFriendActivityNotificationHandlerTest"`

**Checkpoint**: US1 debe publicar altas y bajas de actividad para amigos confirmados.

---

## Phase 4: User Story 2 - Mantener consistencia entre carga inicial y cambios posteriores (Priority: P2)

**Goal**: Enviar snapshot `FRIEND_ACTIVITY_STATE` al suscribirse a social para reconciliar la vista.

**Independent Test**: Un usuario se suscribe a `/user/queue/social` despues de cambios de estado y
recibe un snapshot completo con el estado mas reciente.

### Tests for User Story 2

- [x] T018 [P] [US2] Crear test del caso de uso de snapshot para amigos jugando y no jugando en
  `src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendActivityQueryHandlerTest.java`
- [x] T019 [P] [US2] Crear test del listener de suscripcion social que envia `FRIEND_ACTIVITY_STATE`
  en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListenerTest.java`
- [x] T020 [P] [US2] Crear test de contrato de payload `FRIEND_ACTIVITY_STATE` en
  `src/test/java/com/villo/truco/social/application/events/FriendActivityContractTest.java`

### Implementation for User Story 2

- [x] T021 [US2] Implementar `GetFriendActivityQueryHandler` usando `SocialUserGuard` y
  `FriendActivityResolver` en
  `src/main/java/com/villo/truco/social/application/usecases/queries/GetFriendActivityQueryHandler.java`
- [x] T022 [US2] Registrar `GetFriendActivityUseCase` y `FriendActivityResolver` en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialUseCaseConfiguration.java`
- [x] T023 [US2] Implementar `SocialSubscribeEventListener` para detectar suscripciones a
  `/user/queue/social` y enviar snapshot al usuario autenticado en
  `src/main/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListener.java`
- [x] T024 [US2] Garantizar que `FRIEND_ACTIVITY_STATE` use payload `{ friends: [...] }` en
  `src/main/java/com/villo/truco/social/application/events/FriendActivityNotification.java`
- [x] T025 [US2] Ejecutar tests de US2 con
  `.\gradlew.bat test --tests "*GetFriendActivityQueryHandlerTest" --tests "*SocialSubscribeEventListenerTest" --tests "*FriendActivityContractTest"`

**Checkpoint**: US1 y US2 deben permitir bootstrap, reconciliacion y deltas sin duplicar amigos.

---

## Phase 5: User Story 3 - Limitar novedades a amistades vigentes (Priority: P3)

**Goal**: Evitar actividad para usuarios no amigos, solicitudes pendientes o amistades removidas.

**Independent Test**: Un usuario no recibe actividad de no-amigos; al remover una amistad, la
actividad deja de publicarse para esa relacion.

### Tests for User Story 3

- [x] T026 [P] [US3] Agregar casos de no-amigos, solicitudes pendientes y amistades removidas en
  `src/test/java/com/villo/truco/social/application/services/FriendActivityResolverTest.java`
- [x] T027 [P] [US3] Agregar casos de destinatarios filtrados por amistad aceptada vigente en
  `src/test/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslatorTest.java`
- [x] T028 [P] [US3] Agregar test de repositorio para consultas necesarias de amistades aceptadas en
  `src/test/java/com/villo/truco/social/infrastructure/persistence/repositories/JpaFriendshipRepositoryAdapterTest.java`

### Implementation for User Story 3

- [x] T029 [US3] Ajustar `FriendActivityResolver` para excluir relaciones no aceptadas y remover
  disponibilidad cuando no haya amistad vigente en
  `src/main/java/com/villo/truco/social/application/services/FriendActivityResolver.java`
- [x] T030 [US3] Ajustar `FriendActivityMatchEventTranslator` para publicar deltas solo a amigos
  aceptados vigentes de los jugadores del match en
  `src/main/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslator.java`
- [x] T031 [US3] Implementar consultas de persistencia adicionales si T009 las definio en
  `src/main/java/com/villo/truco/social/infrastructure/persistence/repositories/JpaFriendshipRepositoryAdapter.java`
- [x] T032 [US3] Implementar queries Spring Data adicionales si T009 las definio en
  `src/main/java/com/villo/truco/social/infrastructure/persistence/repositories/spring/SpringDataFriendshipRepository.java`
- [x] T033 [US3] Ejecutar tests de US3 con
  `.\gradlew.bat test --tests "*FriendActivityResolverTest" --tests "*FriendActivityMatchEventTranslatorTest" --tests "*JpaFriendshipRepositoryAdapterTest"`

**Checkpoint**: Todas las historias deben ser funcionales y respetar privacidad social.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentacion, contrato autoritativo y verificacion completa.

- [x] T034 [P] Documentar `FRIEND_ACTIVITY_STATE` y `FRIEND_ACTIVITY_CHANGED` en
  `docs/CONTRATOS_API.md`
- [x] T035 [P] Actualizar README con flujo de actividad social en vivo y relacion con spectate en
  `README.md`
- [x] T036 [P] Actualizar quickstart si la implementacion cambia pasos o nombres de eventos en
  `specs/011-friend-activity-push/quickstart.md`
- [x] T037 Ejecutar suite completa con `.\gradlew.bat test`
- [x] T038 Revisar cumplimiento de arquitectura y privacidad en
  `src/test/java/com/villo/truco/architecture/CleanArchitectureTest.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Phase 1; bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Phase 2; MVP.
- **US2 (Phase 4)**: Depende de Phase 2; puede avanzar en paralelo con US1 despues de acordar los
  DTOs, pero su validacion completa se beneficia de US1.
- **US3 (Phase 5)**: Depende de Phase 2; debe integrarse con US1 para filtrar destinatarios.
- **Polish (Phase 6)**: Depende de las historias implementadas.

### User Story Dependencies

- **US1 (P1)**: Independiente despues de Foundation. Entrega los deltas en vivo.
- **US2 (P2)**: Independiente para snapshot, pero comparte DTOs y handler STOMP con US1.
- **US3 (P3)**: Refuerza privacidad de US1/US2 y debe ejecutarse antes de cerrar la feature.

### Within Each User Story

- Escribir tests primero y verificar que fallan.
- Implementar DTOs/modelos antes de servicios.
- Implementar servicios antes de listeners/configuracion.
- Ejecutar tests de la historia antes de pasar a la siguiente fase.

### Parallel Opportunities

- T002 y T003 pueden hacerse en paralelo.
- T010, T011 y T012 pueden escribirse en paralelo.
- T018, T019 y T020 pueden escribirse en paralelo.
- T026, T027 y T028 pueden escribirse en paralelo.
- T034, T035 y T036 pueden hacerse en paralelo.

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo por archivo:
Task: "Crear test de resolucion de actividad con match IN_PROGRESS y match finalizado en src/test/java/com/villo/truco/social/application/services/FriendActivityResolverTest.java"
Task: "Crear test de traduccion de eventos de match a FRIEND_ACTIVITY_CHANGED en src/test/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslatorTest.java"
Task: "Crear test de publicacion STOMP de FriendActivityNotification en src/test/java/com/villo/truco/social/infrastructure/websocket/StompSocialNotificationHandlerTest.java"
```

## Parallel Example: User Story 2

```bash
# Tests de snapshot/reconexion por archivo:
Task: "Crear test del caso de uso de snapshot para amigos jugando y no jugando en src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendActivityQueryHandlerTest.java"
Task: "Crear test del listener de suscripcion social que envia FRIEND_ACTIVITY_STATE en src/test/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListenerTest.java"
Task: "Crear test de contrato de payload FRIEND_ACTIVITY_STATE en src/test/java/com/villo/truco/social/application/events/FriendActivityContractTest.java"
```

## Parallel Example: User Story 3

```bash
# Tests de privacidad social por capa:
Task: "Agregar casos de no-amigos, solicitudes pendientes y amistades removidas en src/test/java/com/villo/truco/social/application/services/FriendActivityResolverTest.java"
Task: "Agregar casos de destinatarios filtrados por amistad aceptada vigente en src/test/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslatorTest.java"
Task: "Agregar test de repositorio para consultas necesarias de amistades aceptadas en src/test/java/com/villo/truco/social/infrastructure/persistence/repositories/JpaFriendshipRepositoryAdapterTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 y Phase 2.
2. Implementar US1 con tests primero.
3. Validar que un amigo recibe `FRIEND_ACTIVITY_CHANGED` al iniciar y terminar una partida.
4. Detenerse y revisar contrato antes de agregar snapshot.

### Incremental Delivery

1. Setup + Foundation: DTOs y puertos base.
2. US1: deltas en vivo para amigos jugando.
3. US2: snapshot de reconciliacion al suscribirse.
4. US3: privacidad y filtro estricto por amistad vigente.
5. Polish: documentacion y suite completa.

### Parallel Team Strategy

1. Una persona prepara DTOs/puertos base.
2. Otra persona puede escribir tests de US1 mientras se revisan contratos.
3. Despues de Foundation, US1 y US2 pueden avanzar en paralelo si acuerdan
   `FriendActivityNotification`.
4. US3 debe cerrar despues de que exista el flujo de deltas para validar filtrado real.

## Notes

- No crear endpoints REST nuevos para alta de espectador.
- No crear una cola WebSocket por amigo.
- Mantener `/user/queue/social` como superficie social unica.
- Mantener `/user/queue/match-spectate` como flujo exclusivo para registrar espectadores.
- No exponer cartas, acciones privadas ni estado de ronda en eventos sociales.
