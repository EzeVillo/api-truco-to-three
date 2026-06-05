---
description: "Lista de tareas para la implementación de Presencia en tiempo real (push de ocupación)"
---

# Tasks: Presencia en tiempo real (push de ocupación a sesiones activas)

**Input**: Documentos de diseño en `/specs/009-presence-push/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/presence-ws-stream.md](contracts/presence-ws-stream.md)

**Tests**: INCLUIDOS. El Principio III de la constitution exige Test-First con coverage mínimo 70%.
Títulos de tests en español.

**Organization**: Las tareas se agrupan por user story. Cada dominio es un slice independiente que
reutiliza la base común (resolver + notificación + handler STOMP).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1, US2, US3, US4)
- Las descripciones incluyen rutas de archivo exactas

## Convenciones de rutas

- Código: `src/main/java/com/villo/truco/...`
- Tests: `src/test/java/com/villo/truco/...`
- Monolito modular Hexagonal existente (`domain` → `application` → `infrastructure`).

---

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: Confirmar que la estructura de paquetes destino existe; no se crean dependencias ni
módulos nuevos.

- [X] T001 Verificar/crear los paquetes destino: `application/eventhandlers/`,
  `application/events/`, `application/usecases/queries/`, `infrastructure/websocket/dto/`,
  `infrastructure/config/` bajo `src/main/java/com/villo/truco/` (ya existen en su mayoría; no crear
  nada fuera de estos paquetes).

---

## Phase 2: Foundational (Prerequisitos bloqueantes)

**Purpose**: Base común que TODAS las user stories necesitan: el resolver reutilizable de la 008, el
evento de aplicación, el DTO WS y el handler STOMP que efectivamente empuja a
`/user/queue/presence`.

**⚠️ CRITICAL**: Ninguna user story puede empezar hasta completar esta fase.

- [X] T002 [P] Crear el record `PresenceEventNotification` (implementa `ApplicationEvent`) con
  `recipient: PlayerId`, `eventType: String`, `timestamp: Instant`, `snapshot: UserPresenceDTO` en
  `src/main/java/com/villo/truco/application/events/PresenceEventNotification.java`.
- [X] T003 [P] Crear el record `PresenceWsEvent` con `eventType`, `timestamp`, `payload:
  UserPresenceResponse` en
  `src/main/java/com/villo/truco/infrastructure/websocket/dto/PresenceWsEvent.java`.
- [X] T004 Extraer `UserPresenceResolver` (método `resolve(PlayerId): UserPresenceDTO`) con la
  lógica
  hoy en `GetUserPresenceQueryHandler` (resolución de match/league/cup/rematch y reglas de
  `currentMatchId`/coherencia) en
  `src/main/java/com/villo/truco/application/usecases/queries/UserPresenceResolver.java`.
- [X] T005 Refactorizar `GetUserPresenceQueryHandler` para delegar en `UserPresenceResolver` sin
  cambiar comportamiento, en
  `src/main/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandler.java`.
- [X] T006 [P] Test unitario `UserPresenceResolverTest` (cubre busy true/false, currentMatchId solo
  en
  IN_PROGRESS, coherencia match==currentMatchId) en
  `src/test/java/com/villo/truco/application/usecases/queries/UserPresenceResolverTest.java`.
- [X] T007 Ajustar `GetUserPresenceQueryHandlerTest` para el handler ya delegado (mantener verde el
  contrato de la 008) en
  `src/test/java/com/villo/truco/application/usecases/queries/GetUserPresenceQueryHandlerTest.java`.
- [X] T008 Crear `StompPresenceNotificationHandler` (implementa
  `ApplicationEventHandler<PresenceEventNotification>`): mapea `snapshot` → `UserPresenceResponse`,
  arma `PresenceWsEvent` y empuja a `/user/queue/presence` del `recipient` vía
  `SimpMessagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(recipient), "/queue/presence", wsEvent)`;
  registra éxito/fallo en `EventNotifierHealthRegistry`. Archivo:
  `src/main/java/com/villo/truco/infrastructure/websocket/StompPresenceNotificationHandler.java`.
- [X] T009 [P] Test `StompPresenceNotificationHandlerTest`: verifica destino `/user/queue/presence`,
  destinatario correcto y aislamiento (no se envía a otro usuario), con `SimpMessagingTemplate`
  mockeado, en
  `src/test/java/com/villo/truco/infrastructure/websocket/StompPresenceNotificationHandlerTest.java`.
- [X] T010 Crear `PresenceNotificationConfiguration` con los `@Bean` de `UserPresenceResolver` y
  `StompPresenceNotificationHandler`, y registrar el handler STOMP como
  `ApplicationEventHandler` consumidor de `PresenceEventNotification` (siguiendo el wiring de
  `SocialApplicationEventConfiguration`), en
  `src/main/java/com/villo/truco/infrastructure/config/PresenceNotificationConfiguration.java`.

**Checkpoint**: Base lista — un `PresenceEventNotification` publicado ya llega a
`/user/queue/presence`. Las user stories pueden empezar.

---

## Phase 3: User Story 1 - La sesión ociosa se entera de la partida (Priority: P1) 🎯 MVP

**Goal**: Cuando el usuario entra a una partida no finalizada, todas sus sesiones reciben el
snapshot
con `match` y derivan ahí.

**Independent Test**: Disparar `PlayerJoinedEvent`/`MatchDerivedEvent` para un usuario con sesión
suscripta a `/user/queue/presence` y verificar que recibe un snapshot con `match.id` y `busy: true`.

### Tests for User Story 1 ⚠️ (escribir primero, deben fallar)

- [X] T011 [P] [US1] Test `MatchPresenceEventTranslatorTest`: ante `PlayerJoinedEvent` se publica un
  `PresenceEventNotification` por cada jugador humano (bots filtrados con `BotRegistry` fake) con el
  snapshot resuelto; un evento sin transición no publica. Archivo:
  `src/test/java/com/villo/truco/application/eventhandlers/MatchPresenceEventTranslatorTest.java`.

### Implementation for User Story 1

- [X] T012 [US1] Crear `MatchPresenceEventTranslator` (implementa
  `MatchDomainEventHandler<MatchDomainEvent>`): filtra los eventos de entrada/liberación de partida
  (ver tabla research §4: `PlayerJoinedEvent`, `MatchDerivedEvent` y, para US4, los de liberación),
  extrae `playerOne`/`playerTwo`, filtra bots con `BotRegistry`, y por cada jugador humano publica
  `PresenceEventNotification(player, resolver.resolve(player))` vía `ApplicationEventPublisher`.
  Archivo:
  `src/main/java/com/villo/truco/application/eventhandlers/MatchPresenceEventTranslator.java`.
- [X] T013 [US1] Registrar el `@Bean MatchPresenceEventTranslator` en
  `PresenceNotificationConfiguration` y **agregarlo a la lista de handlers** del
  `MatchEventNotifier`
  en `src/main/java/com/villo/truco/infrastructure/config/EventNotifierConfiguration.java` (método
  `matchEventNotifier()`).
- [X] T014 [US1] Validar el conjunto exacto de eventos de entrada a partida contra los emisores
  reales
  (¿`MatchDerivedEvent` se emite en quick match y en partidas de torneo? ¿`PlayerJoinedEvent` cubre
  el
  alta?) y ajustar el filtro de `MatchPresenceEventTranslator` en consecuencia.

**Checkpoint**: US1 funcional — entrar a una partida empuja el snapshot a todas las sesiones. **MVP
**.

---

## Phase 4: User Story 2 - Liga/copa que arranca y manda la partida del torneo (Priority: P2)

**Goal**: Al arrancar o avanzar una liga/copa, todas las sesiones reciben el snapshot con
`league`/`cup` y su `currentMatchId`.

**Independent Test**: Disparar `LeagueStartedEvent`/`LeagueMatchActivatedEvent` (y los de copa) y
verificar el snapshot con `currentMatchId` coherente con `match.id`.

### Tests for User Story 2 ⚠️

- [X] T015 [P] [US2] Test `LeaguePresenceEventTranslatorTest`: ante `LeagueStartedEvent` /
  `LeagueMatchActivatedEvent` / `LeagueAdvancedEvent` se publica un snapshot por participante humano
  con `league.currentMatchId`; bots filtrados. Archivo:
  `src/test/java/com/villo/truco/application/eventhandlers/LeaguePresenceEventTranslatorTest.java`.
- [X] T016 [P] [US2] Test `CupPresenceEventTranslatorTest`: análogo para copa
  (`CupStartedEvent`/`CupMatchActivatedEvent`/`CupAdvancedEvent`). Archivo:
  `src/test/java/com/villo/truco/application/eventhandlers/CupPresenceEventTranslatorTest.java`.

### Implementation for User Story 2

- [X] T017 [P] [US2] Crear `LeaguePresenceEventTranslator` (implementa
  `LeagueDomainEventHandler<LeagueDomainEvent>`): filtra entrada/arranque/avance/liberación de liga
  (research §4), toma `participants` (o el `playerId` del evento), filtra bots y publica un snapshot
  por jugador humano. Archivo:
  `src/main/java/com/villo/truco/application/eventhandlers/LeaguePresenceEventTranslator.java`.
- [X] T018 [P] [US2] Crear `CupPresenceEventTranslator` (implementa
  `CupDomainEventHandler<CupDomainEvent>`): análogo a T017 para copa. Archivo:
  `src/main/java/com/villo/truco/application/eventhandlers/CupPresenceEventTranslator.java`.
- [X] T019 [US2] Registrar ambos `@Bean` en `PresenceNotificationConfiguration` y agregarlos a las
  listas de `leagueEventNotifier()` y `cupEventNotifier()` en
  `src/main/java/com/villo/truco/infrastructure/config/EventNotifierConfiguration.java`.
- [X] T020 [US2] Validar contra los emisores reales qué eventos de liga/copa cruzan un borde de
  ocupación (incluido `CupBoutActivatedEvent` si aplica) y ajustar los filtros.

**Checkpoint**: US1 y US2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Se abre una revancha (Priority: P3)

**Goal**: Al abrirse una sesión de revancha, todas las sesiones reciben el snapshot con `rematch`.

**Independent Test**: Disparar `RematchSessionOpenedEvent` y verificar el snapshot con `rematch.id`
y
`rematch.originMatchId`.

### Tests for User Story 3 ⚠️

- [X] T021 [P] [US3] Test `RematchPresenceEventTranslatorTest`: ante `RematchSessionOpenedEvent` se
  publica un snapshot por jugador humano con `rematch`; bots filtrados. Archivo:
  `src/test/java/com/villo/truco/application/eventhandlers/RematchPresenceEventTranslatorTest.java`.

### Implementation for User Story 3

- [X] T022 [US3] Crear `RematchPresenceEventTranslator` (implementa
  `RematchSessionDomainEventHandler<RematchSessionDomainEvent>`): filtra apertura/cierre de revancha
  (research §4), toma `playerOneId`/`playerTwoId`, filtra bots y publica un snapshot por jugador
  humano. Archivo:
  `src/main/java/com/villo/truco/application/eventhandlers/RematchPresenceEventTranslator.java`.
- [X] T023 [US3] Registrar el `@Bean RematchPresenceEventTranslator` en
  `PresenceNotificationConfiguration` y agregarlo a la lista de handlers de
  `rematchSessionEventNotifier(...)` en
  `src/main/java/com/villo/truco/infrastructure/config/RematchConfiguration.java`.

**Checkpoint**: US1, US2 y US3 funcionan de forma independiente.

---

## Phase 6: User Story 4 - El usuario se libera y las sesiones vuelven al home (Priority: P3)

**Goal**: Al terminar la ocupación (partida finaliza, liga/copa concluye, revancha se
resuelve/expira)
las sesiones reciben un snapshot con el dominio en `null` (y `busy: false` si no queda nada).

**Independent Test**: Disparar `MatchFinishedEvent` para un usuario ocupado y verificar el snapshot
con `match: null`.

### Tests for User Story 4 ⚠️

- [X] T024 [P] [US4] Ampliar `MatchPresenceEventTranslatorTest`: ante `MatchFinishedEvent` /
  `MatchCancelledEvent` / `MatchAbandonedEvent` / `MatchForfeitedEvent` / `MatchPlayerLeftEvent` se
  publica un snapshot con `match: null` (y `busy: false` si corresponde), en
  `src/test/java/com/villo/truco/application/eventhandlers/MatchPresenceEventTranslatorTest.java`.
- [X] T025 [P] [US4] Ampliar los tests de liga/copa/rematch para los eventos de liberación
  (`*Finished`/`*Cancelled`/`*PlayerLeft`/`*PlayerForfeited`, `RematchSessionConfirmed`/
  `ClosedByLeave`/`Expired`) verificando el dominio en `null`, en los respectivos
  `*PresenceEventTranslatorTest`.

### Implementation for User Story 4

- [X] T026 [US4] Confirmar que los filtros de los 4 traductores incluyen los eventos de liberación
  de
  research §4 (la liberación se obtiene "gratis" al re-resolver: el repo ya no devuelve el recurso →
  ref `null`). Ajustar los filtros de
  `MatchPresenceEventTranslator`/`LeaguePresenceEventTranslator`/`CupPresenceEventTranslator`/
  `RematchPresenceEventTranslator` si falta algún evento terminal.

**Checkpoint**: Las 4 user stories funcionan de forma independiente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificación end-to-end, documentación y validaciones transversales.

- [X] T027 Test de integración STOMP multi-sesión (extremo a extremo): dos sesiones del mismo
  usuario
  suscriptas a `/user/queue/presence`; al entrar a una partida ambas reciben el snapshot; un segundo
  usuario NO lo recibe (FR-008). Archivo:
  `src/test/java/com/villo/truco/infrastructure/websocket/PresencePushIntegrationTest.java`.
- [X] T028 Verificar `CleanArchitectureTest` (ArchUnit) en verde: los traductores en `application`
  dependen solo de puertos/eventos de `domain` y `application`; el push STOMP solo en
  `infrastructure`. Ajustar si hay violación.
- [X] T029 [P] Actualizar `README.md`: nueva capacidad de tiempo real (push de presencia) y la cola
  `/user/queue/presence`, con el eventType `PRESENCE_UPDATED` y su relación con
  `GET /api/me/presence`.
- [X] T030 [P] Actualizar `docs/CONTRATOS_API.md`: agregar el stream `/user/queue/presence`
  (eventType `PRESENCE_UPDATED`, payload `UserPresenceResponse`), aclarando que complementa —no
  reemplaza— a `GET /api/me/presence`.
- [X] T031 Ejecutar `./gradlew build` (tests + coverage ≥70% + ArchUnit) y corregir lo que falle.
- [ ] T032 Validar manualmente los escenarios de [quickstart.md](quickstart.md) (P1–P4 +
  aislamiento +
  solo lectura + reconciliación).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. **BLOQUEA** todas las user stories (resolver +
  notificación + DTO WS + handler STOMP + config base).
- **User Stories (Phase 3–6)**: dependen de Foundational. Entre sí son independientes (cada una toca
  su propio traductor y su propio registro en config); pueden hacerse en paralelo.
- **Polish (Phase 7)**: depende de las user stories que se quieran liberar.

### User Story Dependencies

- **US1 (P1)**: solo Foundational. Es el MVP.
- **US2 (P2)**: solo Foundational. Independiente de US1.
- **US3 (P3)**: solo Foundational. Independiente.
- **US4 (P3)**: la liberación se implementa **dentro** de los mismos traductores de US1–US3 (eventos
  terminales). Recomendado hacerla junto con cada traductor; los tests de US4 amplían los de
  US1–US3.

### Within Each User Story

- Tests primero (deben fallar) → traductor → registro en config → validación de eventos reales.

### Parallel Opportunities

- T002 y T003 en paralelo (archivos distintos).
- En Foundational, T006 y T009 son `[P]`.
- US1, US2 y US3 pueden desarrollarse en paralelo tras Foundational.
- Dentro de US2, T017 y T018 (liga/copa) en paralelo; T015 y T016 (tests) en paralelo.
- T029 y T030 (docs) en paralelo.

---

## Parallel Example: Foundational

```text
# Tras T004/T005 (resolver), correr en paralelo:
Task: "T006 [P] UserPresenceResolverTest"
Task: "T009 [P] StompPresenceNotificationHandlerTest"
# y desde el inicio de la fase:
Task: "T002 [P] PresenceEventNotification"
Task: "T003 [P] PresenceWsEvent"
```

## Parallel Example: User Stories en paralelo

```text
# Tras completar Foundational, distintos desarrolladores:
Dev A: US1 (MatchPresenceEventTranslator) — T011, T012, T013, T014
Dev B: US2 (League/Cup translators)        — T015–T020
Dev C: US3 (RematchPresenceEventTranslator)— T021, T022, T023
```

---

## Implementation Strategy

### MVP First (solo US1)

1. Phase 1: Setup.
2. Phase 2: Foundational (CRÍTICO — bloquea todo).
3. Phase 3: US1 (incluida la liberación de partida de US4 en el mismo traductor).
4. **PARAR y VALIDAR**: probar US1 con dos sesiones (quickstart P1).
5. Liberar/demo.

### Incremental Delivery

1. Setup + Foundational → base lista.
2. US1 (+ liberación de partida) → test → demo (MVP).
3. US2 (liga/copa) → test → demo.
4. US3 (revancha) → test → demo.
5. US4 (liberación en todos los dominios) → test → demo.

---

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes.
- La liberación (US4) se obtiene re-resolviendo la presencia tras un evento terminal: el repo deja
  de
  devolver el recurso → ref `null`. No requiere lógica nueva, solo incluir los eventos terminales en
  los filtros.
- El payload es siempre el **snapshot completo** (forma de `UserPresenceResponse`), garantizando
  coherencia (FR-013) y reconciliación con la 008 (FR-011).
- Bots filtrados con `BotRegistry` en todos los traductores.
- Commit después de cada tarea o grupo lógico.
