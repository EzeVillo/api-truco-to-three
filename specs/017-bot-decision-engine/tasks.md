---
description: "Lista de tareas — Motor de decisión del bot por aritmética del match a 3"
---

# Tasks: Motor de decisión del bot por aritmética del match a 3

**Input**: Documentos de diseño en `specs/017-bot-decision-engine/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/decision-rule-spi.md

**Tests**: INCLUIDOS. El Principio III de la constitution exige Test-First con cobertura ≥ 70%; los
títulos de tests van en español.

**Organización**: tareas agrupadas por historia de usuario para implementación y prueba
independientes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia de usuario asociada (US1, US2, US3)
- Rutas de archivo exactas en cada descripción

## Path Conventions

- Backend Java single project: `src/main/java/...`, `src/test/java/...`
- Paquete del motor: `com.villo.truco.domain.model.bot`
- Subpaquete nuevo: `com.villo.truco.domain.model.bot.decision` (+ `.decision.rules`)

---

## Phase 1: Setup (Infraestructura compartida)

**Purpose**: crear la estructura del subpaquete nuevo.

- [X] T001 Crear el subpaquete `decision` y `decision/rules` con `package-info.java` (resumen en
  español del propósito del motor) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/package-info.java` y
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/package-info.java`

---

## Phase 2: Foundational (Prerrequisitos bloqueantes)

**Purpose**: SPI + primitivas determinísticas + proveedores de probabilidad + registry + fallback de
VE + reescritura del motor. Al terminar esta fase el bot ya **juega por valor esperado** con una
jugada legal en toda posición (FR-018), sin las tácticas forzadas todavía.

**⚠️ CRÍTICO**: ninguna historia de usuario puede empezar hasta completar esta fase.

### Primitivas determinísticas (info cierta)

- [X] T002 [P] Tests de `MatchArithmetic` (rival/bot se pasa si acepta/gana/rechaza, llega exacto,
  gana match si pierdo) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/MatchArithmeticTest.java`
- [X] T003 Implementar `MatchArithmetic` (generaliza `TrucoScoreStrategy`, opera sobre
  `myScore/rivalScore/pointsToWin` y stakes) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/MatchArithmetic.java`
- [X] T004 [P] Tests de `CardLockAnalyzer` (rival sin cartas, no puede QYMVAM, mata carta jugada,
  encierro al avanzar, parda) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/CardLockAnalyzerTest.java`
- [X] T005 Implementar `CardLockAnalyzer` (deriva de `BotMatchView.GameContext`: `rivalCardsInHand`,
  `myCards`, `rivalCardPlayed`, `handsPlayedCount`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/CardLockAnalyzer.java`

### Proveedores de probabilidad (única info oculta)

- [X] T006 [P] Tests de `TantoProbabilityProvider` (más probable ganar/perder/empate, delega en
  `EnvidoProbabilityCalculator`) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/TantoProbabilityProviderTest.java`
- [X] T007 Implementar `TantoProbabilityProvider` (fachada sobre `EnvidoProbabilityCalculator` +
  `EnvidoScoring`, con `moreLikelyToLoseTanto()/moreLikelyToWinTanto()/tie()`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/TantoProbabilityProvider.java`
- [X] T008 [P] Tests de `UnplayedHandProbability` (prob. de que la carta alta gane una mano no
  jugada, enumerando mazo restante) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/UnplayedHandProbabilityTest.java`
- [X] T009 Implementar `UnplayedHandProbability` (calculadora combinatoria estilo
  `EnvidoProbabilityCalculator`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/UnplayedHandProbability.java`

### SPI, contexto, registry y fallback

- [X] T010 [P] Definir SPI `DecisionRule` (`Optional<BotAction> apply(DecisionContext)`,
  `int priority()`, `String name()`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/DecisionRule.java`
- [X] T011 Implementar `DecisionContext` (VO inmutable: `view`, `arithmetic`, `lock`, `tanto`,
  `unplayedHand`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/DecisionContext.java` (depende de T003,
  T005, T007, T009, T010)
- [X] T012 [P] Tests de `ExpectedValueFallbackRule` (siempre devuelve acción legal; personalidad
  modula faroles; nunca QYMVAM sin cartas; nunca VE negativo) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/ExpectedValueFallbackRuleTest.java`
- [X] T013 Implementar `ExpectedValueFallbackRule` (Caso 8; reutiliza `HandStrengthEvaluator` +
  `CardSelectionPolicy` + `BotPersonality` + `Random`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/ExpectedValueFallbackRule.java` (
  depende de T011)
- [X] T014 [P] Tests de `DecisionRuleRegistry` (evalúa por prioridad, primer `Optional` no vacío
  gana, siempre resuelve vía fallback) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/DecisionRuleRegistryTest.java`
- [X] T015 Implementar `DecisionRuleRegistry` (lista ordenada por `priority()`, `decide(ctx)`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/DecisionRuleRegistry.java` (depende de
  T010, T013)

### Reescritura del motor (manteniendo el contrato externo)

- [X] T016 Reescribir `BotDecisionEngine.decide(view)` para construir `DecisionContext` y correr el
  `DecisionRuleRegistry`, **conservando la firma del constructor** `(BotPersonality, EnvidoScoring)`
  para no tocar call sites en
  `src/main/java/com/villo/truco/domain/model/bot/BotDecisionEngine.java` (depende de T011, T015)
- [X] T017 Adaptar `BotDecisionEngineTest` existente al nuevo flujo (smoke: el motor devuelve acción
  legal por VE) en `src/test/java/com/villo/truco/domain/model/bot/BotDecisionEngineTest.java` (
  depende de T016)
- [X] T018 Verificar que la app compila y que `ExecuteBotTurnCommandHandler` y
  `AdvanceBotVsBotMatchCommandHandler` siguen invocando el motor sin cambios (
  `./gradlew test --tests "com.villo.truco.application.*Bot*"`) (depende de T016)

**Checkpoint**: el bot ya decide por valor esperado en cualquier posición; base lista para las
tácticas forzadas.

---

## Phase 3: User Story 1 - El bot fuerza al rival a pasarse del límite (Priority: P1) 🎯 MVP

**Goal**: con la aritmética del marcador, elegir el canto/respuesta que deja al rival sin salida (se
pasa de 3 o no le conviene aceptar). Cubre los Casos 1, 4 y 5.

**Independent Test**: construir posiciones de marcador + tanto conocido y verificar que el bot canta
envido/falta/real según probabilidad y que responde no quiero/QYMVAM cuando eso pasa al rival, sin
depender del encierro de cartas.

### Tests for User Story 1 ⚠️

- [X] T019 [P] [US1] Tests de `ResponseToRivalCallRule` (Caso 5: "no quiero" o QYMVAM que pasa al
  rival; nunca QYMVAM sin cartas) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/ResponseToRivalCallRuleTest.java`
- [X] T020 [P] [US1] Tests de `ForceRivalBustRule` (Caso 4: 2-1 arriba con prob. alta de perder →
  nivel de envido que obliga a aceptar y pasa al rival; umbral configurable) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/ForceRivalBustRuleTest.java`
- [X] T021 [P] [US1] Tests de `EnvidoAtTwoTwoRule` (Caso 1: 2-2 canta sí o sí; envido si más
  probable perder, falta si más probable ganar; empate 50/50 → falta) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/EnvidoAtTwoTwoRuleTest.java`

### Implementation for User Story 1

- [X] T022 [P] [US1] Implementar `ResponseToRivalCallRule` (usa `MatchArithmetic` +
  `CardLockAnalyzer` para legalidad de QYMVAM) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/ResponseToRivalCallRule.java`
- [X] T023 [P] [US1] Implementar `ForceRivalBustRule` con constante
  `HIGH_LOSS_PROBABILITY_THRESHOLD` (0.70) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/ForceRivalBustRule.java`
- [X] T024 [P] [US1] Implementar `EnvidoAtTwoTwoRule` con desempate documentado (50/50 → falta
  envido) en `src/main/java/com/villo/truco/domain/model/bot/decision/rules/EnvidoAtTwoTwoRule.java`
- [X] T025 [US1] Registrar las tres reglas en `DecisionRuleRegistry` con prioridad superior al
  fallback de VE en
  `src/main/java/com/villo/truco/domain/model/bot/decision/DecisionRuleRegistry.java` (depende de
  T022, T023, T024)
- [X] T026 [US1] Tests de integración del pipeline para los Casos 1, 4 y 5 en
  `src/test/java/com/villo/truco/domain/model/bot/BotDecisionEngineTest.java` (depende de T025)

**Checkpoint**: US1 funcional e independientemente testeable (forzar que el rival se pase).

---

## Phase 4: User Story 2 - El bot encierra al rival dejándolo sin cartas (Priority: P1)

**Goal**: reconocer líneas de cartas que dejan al bot con carta y al rival sin cartas; postergar el
canto y avanzar cuando conviene, y cantar truco sabiendo que el rival no puede QYMVAM. Cubre los
Casos 2 y 3.

**Independent Test**: manos donde el bot mata la primera carta del rival; verificar que (a) no canta
y avanza, y (b) gana o canta truco e irse al mazo en la 3ª mano según mate o no la última carta.

### Tests for User Story 2 ⚠️

- [X] T027 [P] [US2] Tests de `LockAndMazoRule` (Caso 2: avanzar sin cantar; ganar si mata la
  última; cantar truco + irse al mazo si no la mata; rival no puede QYMVAM) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/LockAndMazoRuleTest.java`

### Implementation for User Story 2

- [X] T028 [US2] Implementar `LockAndMazoRule` (usa `CardLockAnalyzer`: `leadsToLockIfAdvance`,
  `botHasGuaranteedTrick`, `rivalCannotQYMVAM`) en
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/LockAndMazoRule.java`
- [X] T029 [US2] Registrar `LockAndMazoRule` en `DecisionRuleRegistry` con su prioridad relativa a
  las reglas de US1 en
  `src/main/java/com/villo/truco/domain/model/bot/decision/DecisionRuleRegistry.java` (depende de
  T028)
- [X] T030 [US2] Tests de integración del pipeline para el Caso 2 y el Caso 3 (2-1 abajo:
  `ForceRivalBustRule` → `LockAndMazoRule`) en
  `src/test/java/com/villo/truco/domain/model/bot/BotDecisionEngineTest.java` (depende de T029,
  T025)

**Checkpoint**: US1 + US2 funcionan de forma independiente (forzar + encierro).

---

## Phase 5: User Story 3 - Escalera de cantos para encerrar desde el inicio (Priority: P2)

**Goal**: usar niveles altos de truco (retruco/vale cuatro) y la probabilidad de mano no jugada para
encerrar desde 1-1 y 0-0. Cubre los Casos 6 y 7.

**Independent Test**: posiciones 1-1 (rival canta truco) y 0-0 (vale cuatro disponible); verificar
que el bot acepta/escala hacia el encierro y, si la 1ª mano no se jugó, apuesta según
`UnplayedHandProbability`.

### Tests for User Story 3 ⚠️

- [X] T031 [P] [US3] Tests de la lógica de escalera/aceptación en `LockAndMazoRule` (Caso 7: 0-0
  vale cuatro; encadenar cantos para dejar sin respuesta) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/LockAndMazoRuleEscalationTest.java`
- [X] T032 [P] [US3] Tests de aceptación con mano no jugada (Caso 6: 1-1 rival canta truco; aceptar
  por encierro; apostar a la 1ª mano según probabilidad) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/rules/LockAndMazoRuleUnplayedHandTest.java`

### Implementation for User Story 3

- [X] T033 [US3] Extender `LockAndMazoRule` con la escalera de cantos (retruco/vale cuatro) y el uso
  de `UnplayedHandProbability` para la mano no jugada en
  `src/main/java/com/villo/truco/domain/model/bot/decision/rules/LockAndMazoRule.java` (depende de
  T028)
- [X] T034 [US3] Tests de integración del pipeline para los Casos 6 y 7 en
  `src/test/java/com/villo/truco/domain/model/bot/BotDecisionEngineTest.java` (depende de T033)

**Checkpoint**: las tres historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: limpieza, validación de criterios de éxito y documentación.

- [X] T035 [P] Migrar/retirar `TrucoDecisionPolicy` y `EnvidoDecisionPolicy` obsoletas si el
  fallback de VE las subsume, o reducirlas a colaboradores del fallback; ajustar `BotDecisionEngine`
  en `src/main/java/com/villo/truco/domain/model/bot/`
- [X] T036 [P] Test de regresión por valor esperado (SC-004: 0 jugadas de VE negativo) y límite
  determinístico (SC-005: mismo input → misma salida sin `Random`) en
  `src/test/java/com/villo/truco/domain/model/bot/decision/ExpectedValueInvariantsTest.java`
- [X] T037 Validar SC-007: enfrentar el motor rediseñado vs. el anterior en match a 3 y medir
  win-rate (objetivo ≥ 60%) reutilizando el camino bot-vs-bot (
  `AdvanceBotVsBotMatchCommandHandler`); documentar la muestra en
  `specs/017-bot-decision-engine/research.md`
- [X] T038 [P] Aclarar en `specs/017-bot-decision-engine/data-model.md` y
  `contracts/decision-rule-spi.md` que `MatchArithmetic` es solo primitivas (no un gate) y agregar
  el diagrama de prioridades del registry
- [X] T039 Correr `./gradlew build` y verificar cobertura JaCoCo ≥ 70% y reglas ArchUnit (dominio
  puro) en verde
- [X] T040 Ejecutar la validación de `specs/017-bot-decision-engine/quickstart.md` (agregar una
  casuística de prueba como `2-0` y confirmar que es aditiva: nueva regla + registro, sin tocar
  reglas existentes)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup. BLOQUEA todas las historias.
- **US1 (Phase 3)**: depende de Foundational. MVP.
- **US2 (Phase 4)**: depende de Foundational; el Caso 3 (T030) reutiliza `ForceRivalBustRule` de
  US1 (T025).
- **US3 (Phase 5)**: depende de Foundational y de `LockAndMazoRule` (US2, T028).
- **Polish (Phase 6)**: depende de las historias deseadas.

### Within Each User Story

- Los tests se escriben primero y deben FALLAR antes de implementar.
- Primitivas/proveedores antes que reglas; reglas antes de registrar; registro antes de la prueba de
  pipeline.

### Parallel Opportunities

- Foundational: T002/T004/T006/T008/T010 (tests y SPI) en paralelo; luego sus implementaciones.
- US1: T019/T020/T021 (tests) en paralelo; T022/T023/T024 (reglas, archivos distintos) en paralelo.
- US3: T031/T032 (tests) en paralelo.
- Polish: T035/T036/T038 en paralelo.

---

## Parallel Example: User Story 1

```text
# Tests de US1 juntos:
Task: "Tests de ResponseToRivalCallRule (T019)"
Task: "Tests de ForceRivalBustRule (T020)"
Task: "Tests de EnvidoAtTwoTwoRule (T021)"

# Reglas de US1 juntas (archivos distintos):
Task: "Implementar ResponseToRivalCallRule (T022)"
Task: "Implementar ForceRivalBustRule (T023)"
Task: "Implementar EnvidoAtTwoTwoRule (T024)"
```

---

## Implementation Strategy

### MVP First (US1)

1. Phase 1 (Setup) → Phase 2 (Foundational): el bot ya juega por VE.
2. Phase 3 (US1): forzar que el rival se pase.
3. **PARAR y VALIDAR** US1 de forma independiente (Casos 1, 4, 5).

### Incremental Delivery

1. Foundational → bot funcional por VE.
2. US1 → forzar al rival → demo (MVP).
3. US2 → encierro sin cartas → demo.
4. US3 → escalera 1-1/0-0 → demo.
5. Polish → limpieza, SC-007, docs, cobertura.

---

## Notes

- [P] = archivos distintos, sin dependencias pendientes.
- La firma del constructor del motor se conserva → sin cambios en la capa de aplicación (research
  D9).
- Cada casuística futura (p. ej. 2-0) es aditiva: nueva `DecisionRule` + registro, sin editar reglas
  existentes (FR-016 / contrato del SPI).
- Títulos de tests en español; commitear por tarea o grupo lógico.
