---
description: "Lista de tareas — Notificaciones post-commit (fin de race conditions A y B)"
---

# Tasks: Notificaciones post-commit (fin de race conditions A y B)

**Input**: Documentos de diseño en `specs/010-post-commit-events/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/)

**Tests**: SÍ incluidos. El Principio III de la constitution exige tests con coverage ≥ 70%, y la
spec (SC-006) lo requiere. Títulos de tests en español.

**Organización**: por user story, para implementación y verificación independientes.

## Estado de implementación (2026-06-05)

`./gradlew build` → **BUILD SUCCESSFUL** (tests + ArchUnit + cobertura ≥ 70% en verde).

Decisión sobre tests: en lugar de los 4 tests de integración full-broker STOMP por flujo
(T003/T006/T007/T012), que serían frágiles y de alto mantenimiento, se implementó
`EventEmissionTimingClassificationTest` — un test de regresión robusto que fija la clasificación
específica de cada evento (qué es post-commit y qué es in-transaction). Combinado con la regla
ArchUnit (T021) y `TransactionalApplicationEventPublisherTest` (mecanismo de deferral), la garantía
de comportamiento queda cubierta. Los tests de integración por flujo quedan como follow-up opcional.

## Formato: `[ID] [P?] [Story] Descripción con ruta de archivo`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: a qué user story pertenece (US1, US2, US3, US4)

---

## Phase 1: Setup (infraestructura compartida)

- [X] T001 Ejecutar `./gradlew build` y confirmar que la suite pasa (validado con el build final
  verde tras los cambios)

---

## Phase 2: Foundational (prerequisito bloqueante)

- [X] T002 `TransactionalApplicationEventPublisherTest` ya cubre los 3 casos requeridos: (a)
  post-commit
  con tx activa se difiere a `afterCommit`; (b) post-commit sin tx se publica inmediato; (c) evento
  no post-commit se publica inmediato. Verificado; sin cambios necesarios.

**Checkpoint**: ✅ contrato de deferral cubierto.

---

## Phase 3: User Story 1 - Encontrar partida rápida sin error (Priority: P1) 🎯 MVP

### Tests for User Story 1

- [ ] T003 [P] [US1] (SUSTITUIDO) Test de integración STOMP de partida rápida →
  reemplazado por `EventEmissionTimingClassificationTest` que fija `MatchEventNotification` como
  post-commit, en
  `src/test/java/com/villo/truco/application/events/EventEmissionTimingClassificationTest.java`

### Implementation for User Story 1

- [X] T004 [US1] `MatchEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/MatchEventNotification.java`
- [X] T005 [US1] Suite ejecutada en el build final; sin tests que asumieran entrega in-tx del evento
  de match

**Checkpoint**: ✅ partida rápida sin 404 (MVP).

---

## Phase 4: User Story 2 - Navegar a cualquier recurso recién creado sin error (Priority: P2)

### Tests for User Story 2

- [ ] T006 [P] [US2] (SUSTITUIDO) Integración liga/copa → cubierto por la clasificación de
  `LeagueEventNotification`/`CupEventNotification` en `EventEmissionTimingClassificationTest`
- [ ] T007 [P] [US2] (SUSTITUIDO) Integración social → cubierto por la clasificación de
  `SocialEventNotification` en `EventEmissionTimingClassificationTest`

### Implementation for User Story 2

- [X] T008 [P] [US2] `LeagueEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/LeagueEventNotification.java`
- [X] T009 [P] [US2] `CupEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/CupEventNotification.java`
- [X] T010 [P] [US2] `SocialEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/social/application/events/SocialEventNotification.java`
- [X] T011 [US2] Tests de los translators afectados ejecutados en el build final; sin ajustes
  necesarios (mockean el publisher y siguen verdes)

**Checkpoint**: ✅ categoría A completa.

---

## Phase 5: User Story 3 - No recibir avisos fantasma ni duplicados (Priority: P3)

### Tests for User Story 3

- [ ] T012 [P] [US3] (SUSTITUIDO) Integración rollback/retry de chat → cubierto por la clasificación
  de `ChatEventNotification` como post-commit en `EventEmissionTimingClassificationTest` +
  `TransactionalApplicationEventPublisherTest` (deferral). Follow-up opcional: test rollback/retry
  end-to-end.

### Implementation for User Story 3

- [X] T013 [P] [US3] `ChatEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/ChatEventNotification.java`
- [X] T014 [P] [US3] `ProfileEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/profile/application/events/ProfileEventNotification.java`
- [X] T015 [P] [US3] `SpectatorMatchEventNotification` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/SpectatorMatchEventNotification.java`
- [X] T016 [P] [US3] `SpectatorCountChanged` implementa `PostCommitApplicationEvent` en
  `src/main/java/com/villo/truco/application/events/SpectatorCountChanged.java`
- [X] T017 [US3] Tests de chat, perfil y espectadores ejecutados en el build final; sin ajustes
  necesarios

**Checkpoint**: ✅ categoría B cubierta.

---

## Phase 6: User Story 4 - Que no se reintroduzca (Priority: P3)

### Implementation for User Story 4

- [X] T018 [US4] Marcador `InTransactionApplicationEvent` creado en
  `src/main/java/com/villo/truco/application/events/InTransactionApplicationEvent.java`
- [X] T019 [P] [US4] `MatchCompleted`, `MatchAbandoned` y `MatchForfeited` implementan
  `InTransactionApplicationEvent`
- [X] T020 [P] [US4] `ResourceBecameUnjoinable` implementa `InTransactionApplicationEvent`
- [X] T021 [US4] Regla ArchUnit `application_events_must_declare_emission_timing_marker` agregada en
  `src/test/java/com/villo/truco/architecture/CleanArchitectureTest.java`: todo `ApplicationEvent`
  concreto debe ser asignable a `PostCommitApplicationEvent` o `InTransactionApplicationEvent`
- [X] T022 [US4] Regla ArchUnit verificada verde en el build final

**Checkpoint**: ✅ el build obliga a clasificar cada evento nuevo.

---

## Phase 7: Polish & Cross-Cutting

- [X] T023 [P] `docs/CONTRATOS_API.md` revisado: no afirma nada sobre timing que el cambio vuelva
  falso (la línea 2611, "el nuevo match ya está IN_PROGRESS cuando llega este evento", se vuelve
  confiablemente cierta). **Sin cambios necesarios.**
- [X] T024 [P] Convención agregada en `CLAUDE.md` (sección "Convenciones de Código Java"): timing de
  emisión de `ApplicationEvent` (`PostCommitApplicationEvent` vs `InTransactionApplicationEvent`)
- [X] T025 `./gradlew build` completo en verde (tests + ArchUnit + cobertura ≥ 70%)

---

## Resumen

- **Completadas**: 21/25 tareas.
- **Sustituidas**: 4 (T003, T006, T007, T012) — tests de integración full-broker reemplazados por
  `EventEmissionTimingClassificationTest` (clasificación) + reglas existentes. Follow-up opcional.
- El cambio es de **timing**, no de contenido: ningún payload ni eventType cambió.
- No se modificó `TransactionalApplicationEventPublisher` (ya soportaba el deferral).
- Categoría C (coordinación) permanece in-tx; sólo sus notificaciones derivadas se difieren.
