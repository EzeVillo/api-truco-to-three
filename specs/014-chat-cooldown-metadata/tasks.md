# Tasks: Estado de envio en chat

**Input**: Documentos de diseno desde `specs/014-chat-cooldown-metadata/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/chat-send-state.md](contracts/chat-send-state.md),
[quickstart.md](quickstart.md)

**Tests**: Obligatorios por constitucion del proyecto. Los tests deben escribirse primero y fallar
antes de implementar.

**Organization**: Las tareas estan agrupadas por historia de usuario para permitir implementacion y
validacion independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo porque toca archivos distintos y no depende de tareas
  incompletas.
- **[Story]**: Historia de usuario cubierta por la tarea (`US1`, `US2`, `US3`, `US4`).
- Todas las tareas incluyen rutas exactas.

## Phase 1: Setup (Infraestructura Compartida)

**Purpose**: Preparar contexto y verificar contratos actuales antes de cambiar codigo.

- [X] T001 Revisar el contrato actual de chat en `docs/CONTRATOS_API.md` secciones 7.1, 7.2, 7.3,
  7.4 y registrar divergencias con `specs/014-chat-cooldown-metadata/contracts/chat-send-state.md`
- [X] T002 [P] Revisar los DTOs actuales de chat en
  `src/main/java/com/villo/truco/application/dto/ChatMessagesDTO.java` y
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/ChatMessagesResponse.java`
- [X] T003 [P] Revisar los casos de uso actuales de envio en
  `src/main/java/com/villo/truco/application/ports/in/SendMessageUseCase.java` y
  `src/main/java/com/villo/truco/application/ports/in/SendMessageToParentUseCase.java`

---

## Phase 2: Foundational (Prerrequisitos Bloqueantes)

**Purpose**: Crear el modelo compartido de estado de envio y habilitar su calculo antes de historias
REST concretas.

**CRITICO**: Ninguna historia de usuario debe implementarse antes de completar esta fase.

- [X] T004 [P] Agregar tests de dominio para `canSendNow=true`, `canSendNow=false` y
  `nextMessageAllowedAt` en `src/test/java/com/villo/truco/domain/model/chat/ChatTest.java`
- [X] T005 Agregar modelo de lectura de estado de envio en
  `src/main/java/com/villo/truco/domain/model/chat/ChatSendStateView.java`
- [X] T006 Agregar metodo de calculo de estado de envio por jugador en
  `src/main/java/com/villo/truco/domain/model/chat/Chat.java`
- [X] T007 Agregar DTO de aplicacion `ChatSendStateDTO` en
  `src/main/java/com/villo/truco/application/dto/ChatSendStateDTO.java`
- [X] T008 Agregar response HTTP `ChatSendStateResponse` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/ChatSendStateResponse.java`
- [X] T009 Actualizar assembler para proyectar `ChatSendStateView` a `ChatSendStateDTO` en
  `src/main/java/com/villo/truco/application/assemblers/ChatMessagesDTOAssembler.java`
- [X] T010 Ejecutar tests de dominio de chat con `.\gradlew.bat test --tests "*ChatTest"`

**Checkpoint**: El dominio puede calcular estado de envio sin Spring y el modelo compartido esta
disponible para queries y commands.

---

## Phase 3: User Story 1 - Saber si puedo escribir al abrir un chat (Priority: P1) MVP

**Goal**: Las lecturas de chat incluyen `sendState` para el jugador autenticado, incluso despues de
un refresh durante cooldown.

**Independent Test**: Enviar un mensaje, consultar el chat antes de 2 segundos y verificar que el
GET
informa `canSendNow=false` y `nextMessageAllowedAt`; consultar luego del cooldown y verificar
`canSendNow=true`.

### Tests for User Story 1

- [X] T011 [P] [US1] Agregar tests de query para `GET /api/chats/{chatId}/messages` con `sendState`
  en
  `src/test/java/com/villo/truco/application/usecases/queries/GetChatMessagesQueryHandlerTest.java`
- [X] T012 [P] [US1] Agregar tests de query para `GET /api/chats/by-parent/{parentType}/{parentId}`
  con `sendState` en
  `src/test/java/com/villo/truco/application/usecases/queries/GetChatByParentQueryHandlerTest.java`
- [X] T013 [P] [US1] Agregar tests HTTP de lectura de chat con `sendState` en
  `src/test/java/com/villo/truco/infrastructure/http/ChatControllerTest.java`

### Implementation for User Story 1

- [X] T014 [US1] Actualizar `ChatMessagesDTO` para incluir `sendState` en
  `src/main/java/com/villo/truco/application/dto/ChatMessagesDTO.java`
- [X] T015 [US1] Actualizar `ChatMessagesDTOAssembler` para recibir el jugador solicitante y
  calcular `sendState` en
  `src/main/java/com/villo/truco/application/assemblers/ChatMessagesDTOAssembler.java`
- [X] T016 [US1] Actualizar `GetChatMessagesQueryHandler` para pasar el jugador solicitante al
  assembler en
  `src/main/java/com/villo/truco/application/usecases/queries/GetChatMessagesQueryHandler.java`
- [X] T017 [US1] Actualizar `GetChatByParentQueryHandler` para pasar el jugador solicitante al
  assembler en
  `src/main/java/com/villo/truco/application/usecases/queries/GetChatByParentQueryHandler.java`
- [X] T018 [US1] Actualizar `ChatMessagesResponse` para serializar `sendState` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/ChatMessagesResponse.java`
- [X] T019 [US1] Ajustar OpenAPI de lecturas de chat para documentar `sendState` en
  `src/main/java/com/villo/truco/infrastructure/http/ChatController.java`
- [X] T020 [US1] Ejecutar tests de US1 con
  `.\gradlew.bat test --tests "*GetChatMessagesQueryHandlerTest" --tests "*GetChatByParentQueryHandlerTest" --tests "*ChatControllerTest"`

**Checkpoint**: US1 funciona de forma independiente; el frontend puede refrescar y reconstruir
estado de envio desde el GET.

---

## Phase 4: User Story 2 - Ver cuando puedo volver a escribir tras enviar (Priority: P1)

**Goal**: Las confirmaciones de envio exitoso devuelven `sendState` actualizado para el remitente.

**Independent Test**: Enviar un mensaje valido y verificar que la respuesta HTTP incluye `chatId`,
`sendState.canSendNow=false` y `nextMessageAllowedAt`.

### Tests for User Story 2

- [X] T021 [P] [US2] Agregar tests de command para resultado de `SendMessageUseCase` con `sendState`
  en
  `src/test/java/com/villo/truco/application/usecases/commands/SendMessageCommandHandlerTest.java`
- [X] T022 [US2] Agregar tests de command para resultado de `SendMessageToParentUseCase` con
  `chatId` y `sendState` en
  `src/test/java/com/villo/truco/application/usecases/commands/SendMessageCommandHandlerTest.java`
- [X] T023 [P] [US2] Agregar tests HTTP para `POST /api/chats/{chatId}/messages` con respuesta 200 y
  body en `src/test/java/com/villo/truco/infrastructure/http/ChatControllerTest.java`
- [X] T024 [US2] Agregar tests HTTP para
  `POST /api/chats/by-parent/{parentType}/{parentId}/messages` con respuesta 201 y `sendState` en
  `src/test/java/com/villo/truco/infrastructure/http/ChatControllerTest.java`

### Implementation for User Story 2

- [X] T025 [US2] Crear DTO de resultado `SendMessageResultDTO` en
  `src/main/java/com/villo/truco/application/dto/SendMessageResultDTO.java`
- [X] T026 [US2] Cambiar `SendMessageUseCase` para retornar `SendMessageResultDTO` en
  `src/main/java/com/villo/truco/application/ports/in/SendMessageUseCase.java`
- [X] T027 [US2] Cambiar `SendMessageToParentUseCase` para retornar `SendMessageResultDTO` con
  `chatId` en `src/main/java/com/villo/truco/application/ports/in/SendMessageToParentUseCase.java`
- [X] T028 [US2] Actualizar `SendMessageCommandHandler` para devolver `chatId` y `sendState`
  actualizado en
  `src/main/java/com/villo/truco/application/usecases/commands/SendMessageCommandHandler.java`
- [X] T029 [US2] Actualizar `SendMessageToParentCommandHandler` para devolver `chatId` y `sendState`
  actualizado en
  `src/main/java/com/villo/truco/application/usecases/commands/SendMessageToParentCommandHandler.java`
- [X] T030 [US2] Crear response HTTP `SendMessageResponse` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/SendMessageResponse.java`
- [X] T031 [US2] Actualizar `SendMessageToParentResponse` para incluir `sendState` en
  `src/main/java/com/villo/truco/infrastructure/http/dto/response/SendMessageToParentResponse.java`
- [X] T032 [US2] Actualizar `ChatController` para que `POST /api/chats/{chatId}/messages` responda
  200 con body en `src/main/java/com/villo/truco/infrastructure/http/ChatController.java`
- [X] T033 [US2] Actualizar `ChatController` para que
  `POST /api/chats/by-parent/{parentType}/{parentId}/messages` incluya `sendState` en
  `src/main/java/com/villo/truco/infrastructure/http/ChatController.java`
- [X] T034 [US2] Ejecutar tests de US2 con
  `.\gradlew.bat test --tests "*SendMessageCommandHandlerTest" --tests "*ChatControllerTest"`

**Checkpoint**: US2 funciona de forma independiente; enviar no requiere un GET inmediato para
actualizar el cooldown en UI.

---

## Phase 5: User Story 3 - Entender un bloqueo por rate limit (Priority: P2)

**Goal**: El rechazo por rate limit conserva motivo distinguible y no introduce una segunda fuente
de
cooldown.

**Independent Test**: Enviar dos mensajes en menos de 2 segundos, verificar error distinguible de
rate limit, luego leer el chat y obtener `sendState`.

### Tests for User Story 3

- [X] T035 [P] [US3] Agregar test HTTP de rate limit sin `sendState`, `nextMessageAllowedAt` ni
  `retryAfterMs` en `src/test/java/com/villo/truco/infrastructure/http/ChatControllerTest.java`
- [X] T036 [P] [US3] Mantener test de rate limit concurrente compatible con el nuevo resultado en
  `src/test/java/com/villo/truco/application/usecases/commands/SendMessageCommandHandlerConcurrencyTest.java`

### Implementation for User Story 3

- [X] T037 [US3] Verificar que `GlobalExceptionHandler` no agrega metadata de cooldown a
  `ChatRateLimitExceededException` en
  `src/main/java/com/villo/truco/infrastructure/http/GlobalExceptionHandler.java`
- [X] T038 [US3] Ajustar `SendMessageCommandHandlerConcurrencyTest` por cambio de tipo de retorno
  sin debilitar la cobertura de concurrencia en
  `src/test/java/com/villo/truco/application/usecases/commands/SendMessageCommandHandlerConcurrencyTest.java`
- [X] T039 [US3] Ejecutar tests de US3 con
  `.\gradlew.bat test --tests "*SendMessageCommandHandlerConcurrencyTest" --tests "*ChatControllerTest"`

**Checkpoint**: US3 funciona de forma independiente; el error identifica rate limit y la
reconciliacion sigue siendo GET de chat.

---

## Phase 6: User Story 4 - Mantener los chats entre participantes simples (Priority: P3)

**Goal**: Los eventos `MESSAGE_SENT` no transportan countdowns ni `sendState` ajeno.

**Independent Test**: Enviar un mensaje y verificar que el evento WebSocket de chat conserva su
shape
actual sin `sendState`.

### Tests for User Story 4

- [X] T040 [P] [US4] Agregar test de traductor de eventos para asegurar que `MESSAGE_SENT` no
  incluye `sendState` en
  `src/test/java/com/villo/truco/application/eventhandlers/ChatNotificationEventTranslatorTest.java`

### Implementation for User Story 4

- [X] T041 [US4] Verificar que `ChatEventMapper` mantiene payload `MESSAGE_SENT` sin `sendState` en
  `src/main/java/com/villo/truco/application/eventhandlers/ChatEventMapper.java`
- [X] T042 [US4] Verificar que `ChatWsEvent` no cambia por esta feature en
  `src/main/java/com/villo/truco/infrastructure/websocket/dto/ChatWsEvent.java`
- [X] T043 [US4] Ejecutar tests de US4 con
  `.\gradlew.bat test --tests "*ChatNotificationEventTranslatorTest"`

**Checkpoint**: US4 funciona de forma independiente; otros participantes reciben mensajes sin ruido
de cooldown.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentacion, validacion completa y limpieza final.

- [X] T044 [P] Actualizar contrato de chat en `docs/CONTRATOS_API.md` secciones 7.1, 7.2, 7.3, 7.4 y
  resumen de chat
- [X] T045 [P] Actualizar documentacion general de chat en `README.md`
- [X] T046 [P] Actualizar quickstart de feature si la implementacion difiere del plan en
  `specs/014-chat-cooldown-metadata/quickstart.md`
- [X] T047 Ejecutar suite completo con `.\gradlew.bat test`
- [X] T048 Ejecutar build completo con `.\gradlew.bat build`
- [X] T049 Revisar diff final para confirmar que no se agregaron eventos periodicos, persistencia
  nueva ni campos de cooldown en errores en `src/main/java/com/villo/truco`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Setup; bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Foundational. Es el MVP.
- **US2 (Phase 4)**: Depende de Foundational; puede implementarse despues de US1 o en paralelo si se
  coordinan cambios en DTOs.
- **US3 (Phase 5)**: Depende de US2 para tipos de retorno actualizados y de US1 para reconciliacion
  por GET.
- **US4 (Phase 6)**: Depende de Foundational; puede ejecutarse en paralelo con US1/US2 porque toca
  eventos.
- **Polish (Phase 7)**: Depende de las historias implementadas.

### User Story Dependencies

- **US1 (P1)**: MVP independiente despues de Foundational.
- **US2 (P1)**: Independiente funcionalmente, pero comparte DTOs y controller con US1.
- **US3 (P2)**: Requiere que US1 exista para reconciliar cooldown por lectura.
- **US4 (P3)**: Independiente de REST; valida compatibilidad WebSocket.

### Within Each User Story

- Tests primero; deben fallar antes de implementar.
- Modelos/DTOs antes de handlers/controllers.
- Commands/queries antes de respuestas HTTP.
- Documentacion al final, despues de confirmar shape real.

### Parallel Opportunities

- T002 y T003 pueden hacerse en paralelo.
- T004, T007 y T008 pueden avanzar en paralelo porque tocan dominio, aplicacion e infraestructura
  distintas.
- T011, T012 y T013 pueden escribirse en paralelo.
- T021, T022, T023 y T024 pueden escribirse en paralelo.
- T035 y T036 pueden escribirse en paralelo.
- T040 puede hacerse en paralelo con tareas REST despues de Foundational.
- T044, T045 y T046 pueden hacerse en paralelo durante Polish.

---

## Parallel Example: User Story 1

```text
Task: "T011 [P] [US1] Agregar tests de query para GET por chatId"
Task: "T012 [P] [US1] Agregar tests de query para GET by-parent"
Task: "T013 [P] [US1] Agregar tests HTTP de lectura de chat"
```

## Parallel Example: User Story 2

```text
Task: "T021 [P] [US2] Agregar tests de command para SendMessageUseCase"
Task: "T022 [US2] Agregar tests de command para SendMessageToParentUseCase"
Task: "T023 [P] [US2] Agregar tests HTTP para POST por chatId"
Task: "T024 [US2] Agregar tests HTTP para POST by-parent"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational.
3. Completar Phase 3: US1.
4. Validar que `GET /api/chats/{chatId}/messages` y
   `GET /api/chats/by-parent/{parentType}/{parentId}` informan `sendState`.
5. Frenar y revisar contrato antes de avanzar con cambios de POST.

### Incremental Delivery

1. Setup + Foundational -> calculo de estado disponible.
2. US1 -> refresh del FE puede reconstruir cooldown.
3. US2 -> envio exitoso actualiza cooldown sin GET inmediato.
4. US3 -> rate limit queda distinguible sin duplicar metadata.
5. US4 -> WebSocket conserva compatibilidad.
6. Polish -> documentacion y suite completo.

### Parallel Team Strategy

Con mas de una persona:

1. Completar juntos Foundational.
2. Una persona toma US1 y otra prepara tests de US2.
3. Una tercera persona puede validar US4 porque toca eventos y no cambia REST.
4. Integrar US3 despues de cerrar los tipos de retorno de US2.

---

## Notes

- `[P]` indica archivos distintos y sin dependencia directa.
- `[US1]`, `[US2]`, `[US3]`, `[US4]` mapean a las historias de `spec.md`.
- Evitar agregar `retryAfterMs` o `nextMessageAllowedAt` al body de error por rate limit.
- Evitar cambios en `MESSAGE_SENT` salvo tests que confirmen compatibilidad.
- Mantener titulos y descripciones de tests en espanol.
