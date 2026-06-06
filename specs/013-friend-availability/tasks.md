# Tasks: Disponibilidad de amigos para invitaciones

**Input**: Design documents from `/specs/013-friend-availability/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/friend-availability.md](contracts/friend-availability.md)

**Tests**: Requeridos por la constitution y por el plan. Escribir tests primero y verificar que
fallen antes de implementar.

**Organization**: Tareas agrupadas por user story para permitir implementacion y validacion
independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencia de tareas incompletas)
- **[Story]**: User story asociada (`US1`, `US2`, `US3`, `US4`)
- Todas las tareas incluyen rutas concretas

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar contratos, documentacion base y puntos de integracion compartidos.

- [x] T001 Actualizar `docs/CONTRATOS_API.md` con el contrato REST/WS de
  `specs/013-friend-availability/contracts/friend-availability.md`
- [x] T002 [P] Actualizar `README.md` con la capacidad de disponibilidad social, online y spectate
  opcional
- [x] T003 [P] Revisar
  `src/main/java/com/villo/truco/infrastructure/websocket/WebSocketAuthInterceptor.java` para
  confirmar que `/user/queue/social` sigue habilitado para la feature

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelos, puertos y servicios base que bloquean todas las historias.

**CRITICAL**: Ninguna user story debe implementarse antes de completar esta fase.

- [x] T004 [P] Crear enum `FriendAvailabilityStatus` en
  `src/main/java/com/villo/truco/social/application/dto/FriendAvailabilityStatus.java`
- [x] T005 [P] Crear enum `FriendBusyReason` en
  `src/main/java/com/villo/truco/social/application/dto/FriendBusyReason.java`
- [x] T006 [P] Crear DTO `FriendAvailabilityDTO` en
  `src/main/java/com/villo/truco/social/application/dto/FriendAvailabilityDTO.java`
- [x] T007 [P] Crear DTO `FriendAvailabilityStateDTO` en
  `src/main/java/com/villo/truco/social/application/dto/FriendAvailabilityStateDTO.java`
- [x] T008 [P] Crear evento `FriendAvailabilityNotification` en
  `src/main/java/com/villo/truco/social/application/events/FriendAvailabilityNotification.java`
- [x] T009 [P] Crear puerto `FriendOnlinePresencePort` en
  `src/main/java/com/villo/truco/social/application/ports/out/FriendOnlinePresencePort.java`
- [x] T010 [P] Usar el bus de eventos existente para disponibilidad; no se requiere puerto
  `FriendAvailabilityNotifier`
- [x] T011 Crear servicio `FriendAvailabilityResolver` en
  `src/main/java/com/villo/truco/social/application/services/FriendAvailabilityResolver.java`
- [x] T012 Registrar `FriendAvailabilityResolver` y puertos nuevos en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialUseCaseConfiguration.java`

**Checkpoint**: Modelos y wiring base listos para implementar historias.

---

## Phase 3: User Story 1 - Saber si puedo invitar a un amigo (Priority: P1) MVP

**Goal**: `GET /api/social/friendships` devuelve disponibilidad, motivo de ocupacion y spectate
opcional para cada amigo aceptado.

**Independent Test**: Con dos amigos confirmados, consultar la lista cuando el amigo esta libre y
luego con cada bloqueo real; el estado cambia entre `AVAILABLE` y `BUSY` con `busyReason`.

### Tests for User Story 1

- [x] T013 [P] [US1] Agregar tests de motivos `AVAILABLE`, `IN_MATCH`, `IN_LEAGUE`, `IN_CUP`,
  `OPEN_REMATCH`, `IN_QUICK_QUEUE` en
  `src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java`
- [x] T014 [US1] Agregar tests de pendientes bloqueantes/no bloqueantes en
  `src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java`
- [x] T015 [P] [US1] Agregar test de contrato REST de `GET /api/social/friendships` en
  `src/test/java/com/villo/truco/social/infrastructure/http/FriendshipControllerTest.java` (ya
  cubierto: el test valida `availability`, `busyReason` y `spectatableMatch`)
- [x] T016 [P] [US1] Agregar test de payload social sin datos privados en
  `src/test/java/com/villo/truco/social/application/events/FriendAvailabilityContractTest.java`

### Implementation for User Story 1

- [x] T017 [US1] Extender `PlayerAvailabilityChecker` para exponer motivo bloqueante reutilizable en
  `src/main/java/com/villo/truco/application/usecases/commands/PlayerAvailabilityChecker.java`
- [x] T018 [US1] Incorporar lectura de Quick Match en `FriendAvailabilityResolver` usando
  `src/main/java/com/villo/truco/domain/ports/QuickMatchQueuePort.java`
- [x] T019 [US1] Incorporar reglas de invitaciones/solicitudes pendientes en
  `FriendAvailabilityResolver` usando
  `src/main/java/com/villo/truco/social/domain/ports/ResourceInvitationQueryRepository.java` y
  `src/main/java/com/villo/truco/social/domain/ports/FriendshipQueryRepository.java`
- [x] T020 [US1] Integrar `spectatableMatch` existente en `FriendAvailabilityResolver` usando
  `src/main/java/com/villo/truco/social/application/services/FriendActivityResolver.java`
- [x] T021 [US1] Actualizar `FriendSummaryDTO` para incluir `online`, `availability`, `busyReason` y
  `spectatableMatch` en `src/main/java/com/villo/truco/social/application/dto/FriendSummaryDTO.java`
- [x] T022 [US1] Actualizar `FriendSummaryResponse` para serializar el contrato nuevo en
  `src/main/java/com/villo/truco/social/infrastructure/http/dto/response/FriendSummaryResponse.java`
- [x] T023 [US1] Actualizar `GetFriendsQueryHandler` para resolver disponibilidad en
  `src/main/java/com/villo/truco/social/application/usecases/queries/GetFriendsQueryHandler.java`
- [x] T024 [US1] Actualizar `FriendshipController` para devolver el response ampliado en
  `src/main/java/com/villo/truco/social/infrastructure/http/FriendshipController.java`

**Checkpoint**: US1 completo; REST permite saber si un amigo puede ser invitado.

---

## Phase 4: User Story 2 - Ver cambios de disponibilidad en vivo (Priority: P2)

**Goal**: La cola social emite snapshot y deltas cuando cambia disponibilidad, motivo o partida
espectable.

**Independent Test**: Con la lista social abierta, cambiar el estado bloqueante de un amigo y
verificar `FRIEND_AVAILABILITY_CHANGED` sin recargar.

### Tests for User Story 2

- [x] T025 [P] [US2] Agregar test de snapshot `FRIEND_AVAILABILITY_STATE` en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListenerTest.java`
  (ya cubierto)
- [x] T026 [P] [US2] Agregar test de handler STOMP de delta en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/StompFriendAvailabilityNotificationHandlerTest.java`
- [x] T027 [P] [US2] Agregar test de traduccion de cambios de match a disponibilidad en
  `src/test/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslatorTest.java`
  (ya cubierto)
- [x] T028 [P] [US2] Agregar test de caso de uso snapshot en
  `src/test/java/com/villo/truco/social/application/usecases/queries/GetFriendAvailabilityQueryHandlerTest.java`

### Implementation for User Story 2

- [x] T029 [P] [US2] Crear query `GetFriendAvailabilityQuery` en
  `src/main/java/com/villo/truco/social/application/queries/GetFriendAvailabilityQuery.java`
- [x] T030 [P] [US2] Crear puerto `GetFriendAvailabilityUseCase` en
  `src/main/java/com/villo/truco/social/application/ports/in/GetFriendAvailabilityUseCase.java`
- [x] T031 [US2] Crear handler `GetFriendAvailabilityQueryHandler` en
  `src/main/java/com/villo/truco/social/application/usecases/queries/GetFriendAvailabilityQueryHandler.java`
- [x] T032 [US2] Actualizar `SocialSubscribeEventListener` para enviar `FRIEND_AVAILABILITY_STATE`
  en
  `src/main/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListener.java`
- [x] T033 [US2] Crear `StompFriendAvailabilityNotificationHandler` en
  `src/main/java/com/villo/truco/social/infrastructure/websocket/StompFriendAvailabilityNotificationHandler.java`
- [x] T034 [US2] Registrar notificador social de disponibilidad en
  `src/main/java/com/villo/truco/social/infrastructure/config/SocialApplicationEventConfiguration.java`
- [x] T035 [US2] Actualizar `FriendActivityMatchEventTranslator` para emitir
  `FriendAvailabilityNotification` cuando cambie match/spectate en
  `src/main/java/com/villo/truco/application/eventhandlers/FriendActivityMatchEventTranslator.java`
- [x] T036 [US2] Emitir cambios de disponibilidad al aceptar, cancelar, rechazar o expirar
  invitaciones en
  `src/main/java/com/villo/truco/social/application/eventhandlers/SocialNotificationEventTranslator.java`
  (recalcula y emite delta a ambas partes; test en `SocialNotificationEventTranslatorTest`)

**Checkpoint**: US2 completo; disponibilidad se actualiza en vivo.

---

## Phase 5: User Story 3 - Distinguir online de disponible (Priority: P3)

**Goal**: Cada amigo muestra `online` separado de `availability`, con multiples sesiones y
desconexion eventual.

**Independent Test**: Conectar y desconectar sesiones del amigo; `online` cambia sin alterar por si
solo `availability`.

### Tests for User Story 3

- [x] T037 [P] [US3] Crear tests de multiples sesiones y ultima desconexion en
  `src/test/java/com/villo/truco/infrastructure/websocket/WebSocketSessionPresenceTrackerTest.java`
- [x] T038 [P] [US3] Agregar tests de `online` separado de `availability` en
  `src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java`
- [x] T039 [P] [US3] Agregar test de delta online (transiciones de borde en el tracker +
  `resolveAvailabilityChangesForPlayer` en
  `src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java`)

### Implementation for User Story 3

- [x] T040 [US3] Crear `WebSocketSessionPresenceTracker` en
  `src/main/java/com/villo/truco/infrastructure/websocket/WebSocketSessionPresenceTracker.java`
- [x] T041 [US3] Implementar adaptador `FriendOnlinePresencePort` con
  `WebSocketSessionPresenceTracker` en
  `src/main/java/com/villo/truco/infrastructure/websocket/WebSocketFriendOnlinePresenceAdapter.java`
- [x] T042 [US3] Registrar sesiones en connect/disconnect STOMP en
  `src/main/java/com/villo/truco/infrastructure/websocket/WebSocketSessionPresenceTracker.java`
- [x] T043 [US3] Conectar `FriendAvailabilityResolver` con `FriendOnlinePresencePort` en
  `src/main/java/com/villo/truco/social/application/services/FriendAvailabilityResolver.java`
- [x] T044 [US3] Emitir `FRIEND_AVAILABILITY_CHANGED` cuando cambia online en
  `src/main/java/com/villo/truco/infrastructure/websocket/WebSocketSessionPresenceTracker.java`
  (detecta bordes 0->1 y 1->0 y delega en `FriendPresenceAvailabilityNotifier`)
- [x] T045 [US3] Registrar adapter de presencia online en
  `src/main/java/com/villo/truco/infrastructure/config/PresenceNotificationConfiguration.java`

**Checkpoint**: US3 completo; online funciona como senal separada.

---

## Phase 6: User Story 4 - Respetar privacidad y amistades vigentes (Priority: P4)

**Goal**: Disponibilidad, online y spectate solo se exponen entre amistades aceptadas vigentes.

**Independent Test**: Eliminar amistad o dejar solicitud pendiente y verificar que no se entregan
snapshots ni deltas de disponibilidad.

### Tests for User Story 4

- [x] T046 [P] [US4] Agregar tests de filtro por amistad aceptada en
  `src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java`
  (`onlyExposesAcceptedFriends`, `pairDeltaEmptyWithoutAcceptedFriendship`)
- [x] T047 [P] [US4] Agregar test de no entrega tras eliminar amistad en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/StompFriendAvailabilityNotificationHandlerTest.java`
  (`sendsNothingWithoutRecipients`)
- [x] T048 [P] [US4] Agregar test de no exposicion para solicitudes pendientes en
  `src/test/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListenerTest.java`
  (cubierto a nivel resolver: `findAcceptedByPlayer` excluye pendientes, validado en
  `FriendAvailabilityResolverTest`)

### Implementation for User Story 4

- [x] T049 [US4] Reforzar filtro de amistades `ACCEPTED` en
  `src/main/java/com/villo/truco/social/application/services/FriendAvailabilityResolver.java`
- [x] T050 [US4] Recalcular destinatarios de deltas solo con amistades vigentes en
  `src/main/java/com/villo/truco/social/infrastructure/websocket/StompFriendAvailabilityNotificationHandler.java`
  (redundante: los destinatarios ya se calculan filtrando amistades aceptadas en el resolver —
  `resolveAvailabilityChangesForPlayer`, `resolveAvailabilityChangesByRecipient`,
  `resolveFriendDeltaFor`; el handler solo entrega a `recipients` ya filtrados)
- [x] T051 [US4] Asegurar remocion visual por `FRIENDSHIP_REMOVED` sin nuevos deltas en
  `src/main/java/com/villo/truco/social/application/eventhandlers/SocialEventMapper.java`
  (ya cubierto por el flujo social existente: `mapFriendshipRemoved` emite `FRIENDSHIP_REMOVED` y el
  cliente remueve al amigo; no se generan deltas de disponibilidad para no-amigos)
- [x] T052 [US4] Verificar que `FriendAvailabilityNotification` no contiene datos privados en
  `src/main/java/com/villo/truco/social/application/events/FriendAvailabilityNotification.java`

**Checkpoint**: US4 completo; privacidad social preservada.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentacion, compatibilidad y validacion final.

- [x] T053 [P] Actualizar resumen de eventos sociales en `docs/CONTRATOS_API.md` para
  reemplazar/reconciliar `FRIEND_ACTIVITY_*` con `FRIEND_AVAILABILITY_*`
- [x] T054 [P] Actualizar quickstart o ejemplos del frontend en `README.md` con `availability`,
  `busyReason`, `online` y `spectatableMatch`
- [x] T055 Revisar compatibilidad temporal de `FriendActivityDTO` en
  `src/main/java/com/villo/truco/social/application/dto/FriendActivityDTO.java`
  (se mantiene sin cambios durante la ventana de compatibilidad: `FRIEND_ACTIVITY_*` coexiste con
  `FRIEND_AVAILABILITY_*`; `FriendActivityDTO` solo lleva `friendUsername` + `spectatableMatch`, sin
  datos privados; la disponibilidad usa `FriendAvailabilityDTO`)
- [x] T056 Ejecutar `.\gradlew.bat test` y corregir fallos (suite completa en verde, incluye
  ArchUnit)
- [ ] T057 Ejecutar validacion manual de `specs/013-friend-availability/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; puede iniciar inmediatamente.
- **Foundational (Phase 2)**: Depende de Setup; bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Foundational; MVP recomendado.
- **US2 (Phase 4)**: Depende de US1 para reutilizar el estado de disponibilidad.
- **US3 (Phase 5)**: Depende de Foundational y puede avanzar en paralelo parcial con US1/US2, pero
  su integracion final depende del DTO de US1.
- **US4 (Phase 6)**: Depende de US1 y US2 para validar privacidad sobre REST y WS.
- **Polish (Phase 7)**: Depende de las historias implementadas.

### User Story Dependencies

- **User Story 1 (P1)**: Base MVP, sin dependencias de otras historias.
- **User Story 2 (P2)**: Reutiliza modelos y resolver de US1.
- **User Story 3 (P3)**: Independiente en tracking online, integrado al resolver de US1.
- **User Story 4 (P4)**: Refuerza privacidad sobre estados y eventos de US1/US2/US3.

### Within Each User Story

- Tests antes de implementacion.
- DTOs/enums antes de servicios.
- Servicios antes de controllers/listeners.
- Notificadores antes de validacion STOMP.
- Documentacion final despues de contrato implementado.

### Parallel Opportunities

- T002 y T003 pueden ejecutarse en paralelo con T001.
- T004-T010 pueden ejecutarse en paralelo.
- T013-T016 pueden escribirse en paralelo.
- T025-T028 pueden escribirse en paralelo.
- T037-T039 pueden escribirse en paralelo.
- T046-T048 pueden escribirse en paralelo.
- US3 puede avanzar en tracking de sesiones mientras US2 implementa eventos, coordinando la
  integracion final.

---

## Parallel Example: User Story 1

```text
Task: "Agregar tests de motivos AVAILABLE, IN_MATCH, IN_LEAGUE, IN_CUP, OPEN_REMATCH, IN_QUICK_QUEUE en src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java"
Task: "Agregar test de contrato REST de GET /api/social/friendships en src/test/java/com/villo/truco/social/infrastructure/http/FriendshipControllerTest.java"
Task: "Agregar test de payload social sin datos privados en src/test/java/com/villo/truco/social/application/events/FriendAvailabilityContractTest.java"
```

## Parallel Example: User Story 2

```text
Task: "Agregar test de snapshot FRIEND_AVAILABILITY_STATE en src/test/java/com/villo/truco/social/infrastructure/websocket/SocialSubscribeEventListenerTest.java"
Task: "Agregar test de handler STOMP de delta en src/test/java/com/villo/truco/social/infrastructure/websocket/StompFriendAvailabilityNotificationHandlerTest.java"
Task: "Crear query GetFriendAvailabilityQuery en src/main/java/com/villo/truco/social/application/queries/GetFriendAvailabilityQuery.java"
Task: "Crear puerto GetFriendAvailabilityUseCase en src/main/java/com/villo/truco/social/application/ports/in/GetFriendAvailabilityUseCase.java"
```

## Parallel Example: User Story 3

```text
Task: "Crear tests de multiples sesiones y ultima desconexion en src/test/java/com/villo/truco/infrastructure/websocket/WebSocketSessionPresenceTrackerTest.java"
Task: "Agregar tests de online separado de availability en src/test/java/com/villo/truco/social/application/services/FriendAvailabilityResolverTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 y Phase 2.
2. Implementar US1 con tests primero.
3. Validar `GET /api/social/friendships` contra el contrato.
4. Detener y revisar antes de pasar a eventos en vivo.

### Incremental Delivery

1. US1: REST muestra disponibilidad para invitar.
2. US2: WebSocket mantiene disponibilidad sincronizada.
3. US3: Online aparece separado de disponibilidad.
4. US4: Privacidad y amistades vigentes quedan reforzadas.
5. Polish: documentacion, compatibilidad y suite completa.

### Parallel Team Strategy

Con varios desarrolladores:

1. Equipo completa Setup + Foundational.
2. Developer A: US1 resolver + REST.
3. Developer B: US2 snapshot/deltas WS despues de acordar DTOs.
4. Developer C: US3 tracker online y adapter.
5. Equipo valida US4 y Polish en conjunto.

---

## Notes

- `[P]` significa archivos distintos y sin dependencias directas.
- Cada tarea de historia tiene etiqueta `[US#]`.
- Verificar que los tests fallen antes de implementar.
- No agregar tablas ni canales WebSocket nuevos salvo que una tarea posterior lo justifique
  explicitamente.
- Mantener `online`, `availability` y `spectatableMatch` como conceptos separados.
