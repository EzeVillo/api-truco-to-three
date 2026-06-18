# Research: Motor de decisión del bot por aritmética del match a 3

Fecha: 2026-06-17 · Feature: `017-bot-decision-engine`

Este documento resuelve las decisiones de diseño abiertas antes de modelar datos y contratos. No
quedan marcadores NEEDS CLARIFICATION.

## D1 — Arquitectura del motor: pipeline de tácticas vs. método monolítico

- **Decisión**: Reemplazar el `decide()` monolítico actual (que encadena
  `TrucoDecisionPolicy`/`EnvidoDecisionPolicy` probabilísticas) por un **pipeline ordenado de
  `DecisionRule`**. Cada regla es una función pura `Optional<BotAction> apply(DecisionContext)` con
  un `priority()`. `DecisionRuleRegistry` las evalúa en orden y devuelve la primera acción no vacía;
  la última regla (`ExpectedValueFallbackRule`) siempre produce una jugada legal.
- **Rationale**: Es el diseño que satisface simultáneamente el pedido del usuario (extensibilidad
  para casuísticas como 2-0) y el FR-016 (no cablear caso por caso). Cada principio queda aislado,
  testeable y combinable. Agregar una casuística = registrar una regla nueva, sin tocar las
  existentes (Open/Closed).
- **Alternativas consideradas**:
    - *Árbol de `if/else` dentro de `decide()`*: rechazado — crece sin límite y obliga a re-testear
      lógica estable por cada caso nuevo.
    - *Tabla declarativa marcador→acción*: rechazado — codifica casos por marcador literal, justo lo
      que el FR-016 prohíbe; no captura la dependencia de cartas/encierro.
    - *Minimax / búsqueda completa del árbol de jugadas*: rechazado por ahora — el árbol a 3 es
      corto
      pero la mano del rival es información oculta; el VE por enumeración de tanto/mano-no-jugada da
      el resultado correcto con mucho menos costo y es más legible. Queda como evolución futura tras
      una `DecisionRule` de VE.

## D2 — Separación determinístico vs. probabilístico (SC-005)

- **Decisión**: Dos colaboradores distintos dentro de `DecisionContext`:
    - `MatchArithmetic` + `CardLockAnalyzer`: **100% determinísticos**. Resuelven "¿el rival se pasa
      si acepta/gana/rechaza?", "¿mi carta mata una carta ya jugada?", "¿el rival se queda sin
      cartas
      / no puede QYMVAM?".
    - `TantoProbabilityProvider` y `UnplayedHandProbability`: **únicas fuentes de azar**. Solo se
      consultan cuando la decisión depende del tanto del rival o de una mano todavía no jugada.
- **Rationale**: Hace verificable el SC-005 por construcción: una regla que no inyecta un proveedor
  de probabilidad es, por tipo, determinística. Los tests pueden afirmar repetibilidad (mismo input
  → misma salida) sin `Random`.
- **Alternativas**: mezclar ambos en las policies actuales (estado actual) — rechazado: imposible
  garantizar el límite de uso de probabilidad.

## D3 — Primitivas aritméticas del marcador a 3

- **Decisión**: Centralizar en `MatchArithmetic` (absorbe y generaliza `TrucoScoreStrategy`):
  `rivalBustsIfAccepts`, `rivalBustsIfWins`, `rivalBustsIfRejects`, `botBustsIfAccepts/Wins`,
  `botReachesExactIf...`, `rivalWinsMatchIfRejects`, etc. Operan sobre `(myScore, rivalScore,
  pointsToWin, stakeAccepted/Rejected)`; nada hardcodea el literal 3.
- **Rationale**: El comportamiento "se pasa de 3" debe derivarse de `pointsToWin` para que cambiar
  el formato (o testear con otros límites) no rompa la lógica (Assumption de la spec). Las reglas
  consultan estas primitivas; no recalculan aritmética inline.
- **Alternativas**: dejar la aritmética dispersa en cada policy (estado actual, parcialmente en
  `TrucoScoreStrategy`/`EnvidoDecisionPolicy`) — rechazado: duplica lógica y dificulta agregar
  casuísticas.

## D4 — Detección de encierro (lock) determinística

- **Decisión**: `CardLockAnalyzer` deriva del estado conocido: cartas propias restantes, cartas
  jugadas, `rivalCardsInHand`, y si una carta propia mata una carta ya jugada. Expone:
  `rivalIsOutOfCards()`, `rivalCannotQYMVAM()` (= rival sin cartas), `botHasGuaranteedTrick()` (mata
  la última carta jugada por el rival), `leadsToLockIfAdvance()` (mató la primera y queda con carta
  mientras el rival se queda sin cartas).
- **Rationale**: Cubre los casos 2, 3, 6, 7 con certeza. La legalidad de QYMVAM ("solo con cartas")
  se modela como predicado del analizador, no como heurística.
- **Alternativas**: inferir el encierro probabilísticamente — rechazado: es información cierta
  (FR-003), usar azar sería incorrecto y violaría SC-005.

## D5 — Probabilidad de la mano no jugada (Caso 6 / FR-014)

- **Decisión**: Nueva `UnplayedHandProbability` que estima la probabilidad de que la carta alta del
  bot gane una mano que el rival **aún no jugó**, enumerando las cartas que el rival podría tener
  (mazo restante = 40 menos las conocidas: propias + jugadas). Reusa el patrón combinatorio de
  `EnvidoProbabilityCalculator`.
- **Rationale**: Es la segunda y última fuente legítima de incertidumbre. Mantener el mismo enfoque
  de enumeración garantiza resultados exactos y consistentes con el cálculo del tanto.
- **Alternativas**: heurística de "fuerza de mano" (`HandStrengthEvaluator`) como proxy — se
  conserva solo para desempates de bajo impacto en el fallback de VE, no para la decisión del Caso
    6.

## D6 — Regla de desempate en empate de probabilidad (FR-017 / edge case 2-2 50/50)

- **Decisión**: En 2-2, si P(ganar tanto) == P(perder tanto), tratar el empate como **"no es más
  probable perder"** ⇒ **falta envido** (llegar exacto a 3). Constante documentada en la regla.
- **Rationale**: Determinístico y documentado; alinea con el espíritu del Caso 1 (preferir cerrar el
  match cuando no hay ventaja clara para la trampa de "que se pase").
- **Alternativas**: elegir envido en el empate — rechazado: regalar la iniciativa sin ventaja
  probabilística es peor VE para el bot que intentar cerrar.

## D7 — Umbral de "probabilidad muy alta de perder el tanto" (Caso 4 / FR-012)

- **Decisión**: Umbral **configurable** como constante nombrada en la regla correspondiente
  (`HIGH_LOSS_PROBABILITY_THRESHOLD`, valor inicial 0.70), no cableado como número mágico disperso.
- **Rationale**: La spec lo declara ajustable (Assumption). Centralizarlo permite tunearlo con datos
  de partidas bot-vs-bot (SC-007) sin tocar la lógica.
- **Alternativas**: número mágico inline — rechazado por mantenibilidad y por dificultar el tuning.

## D8 — Rol de las personalidades (`BotPersonality`)

- **Decisión**: La aritmética y el encierro (reglas determinísticas y de VE forzado) **mandan por
  encima** de la personalidad. La personalidad (envidoso, mentiroso, pescador, temerario) solo
  modula decisiones **genuinamente libres/probabilísticas** dentro de `ExpectedValueFallbackRule`
  (p. ej. faroles cuando ninguna rama está forzada).
- **Rationale**: Alinea con la Assumption de la spec y preserva la diferenciación entre bots de
  campaña sin contradecir las jugadas forzadas. Evita romper la API de `BotProfile`/
  `BotPersonality`.
- **Alternativas**: eliminar personalidades — rechazado: rompería la diferenciación de bots de
  campaña y la configuración existente (`BotConfiguration`).

## D9 — Integración y construcción del motor

- **Decisión**: `BotDecisionEngine` mantiene su rol y su punto de entrada `decide(BotMatchView)`.
  Internamente construye `DecisionContext` (view + arithmetic + proveedores de probabilidad) y corre
  el `DecisionRuleRegistry`. Si la firma del constructor cambia (p. ej. para inyectar el registry o
  proveedores), se **actualizan explícitamente todos los call sites**
  (`ExecuteBotTurnCommandHandler`, `AdvanceBotVsBotMatchCommandHandler`, `BotConfiguration`) — sin
  overloads de conveniencia con defaults ocultos.
- **Rationale**: Conserva el contrato externo del motor (la capa de aplicación no cambia su forma de
  invocarlo) y respeta la regla del proyecto de no agregar overloads de conveniencia.
- **Alternativas**: agregar un constructor sobrecargado con defaults — rechazado por la convención
  registrada del proyecto (actualizar call sites explícitamente).

## D10 — Estrategia de testing

- **Decisión**: (a) tests unitarios por **primitiva** (`MatchArithmetic`, `CardLockAnalyzer`) con
  tablas de marcador; (b) tests unitarios por **regla** con `DecisionContext` armado a mano; (c)
  tests de **integración del pipeline** en `BotDecisionEngineTest` que reproducen los 8 casos de la
  spec y verifican la acción exacta; (d) las ramas determinísticas se prueban **sin `Random`**.
- **Rationale**: Cobertura ≥ 70% y trazabilidad 1:1 con los Acceptance Scenarios y SC-001..SC-006.
- **Alternativas**: solo tests de integración — rechazado: no aislaría regresiones por regla ni
  permitiría afirmar el límite determinístico (SC-005).

## D11 — Validación empírica SC-007 (win-rate motor rediseñado vs. anterior)

- **Decisión**: validar SC-007 con un benchmark empírico que enfrenta el motor rediseñado contra una
  réplica del motor anterior (`LegacyBotDecisionEngine`, test-only, copia fiel del código pre-T016)
  reutilizando el camino bot-vs-bot a nivel de dominio: se crea un `Match` (partida única a 3
  puntos, `MatchRules(1, false)`), y por cada turno se resuelve qué bot actúa
  (`hasAvailableActions()`), se traduce la vista con `MatchToBotACL` y se despacha la `BotAction` al
  match — exactamente como `ExecuteBotTurnCommandHandler` + `AdvanceBotVsBotMatchCommandHandler`.
  Implementado en `BotEngineHeadToHeadBenchmarkTest` (alternando seats para anular el sesgo de
  mano; `@Disabled` porque el reparto y los motores no son deterministas).
- **Muestra**: 120 partidas, 0 errores, todas completadas. Win-rate observado del motor
  rediseñado: **≈ 0,29 (29%)**, claramente por debajo del objetivo SC-007 (≥ 60%).
- **Conclusión**: **SC-007 NO cumplido** con la implementación actual. El motor rediseñado pierde
  frente a la baseline anterior en juego empírico.
- **Análisis de causa probable**: la diferencia entre ambos motores es únicamente el conjunto de
  reglas forzadas (`ResponseToRivalCall`, `EnvidoAtTwoTwo`, `ForceRivalBust`, `LockAndMazo`) sobre
  un fallback de VE idéntico al motor anterior. Las reglas de envido (Casos 1/4) son
  especificadas y unitariamente correctas, pero de alta varianza. El culpable más likely es la
  **escalada de `LockAndMazoRule` (Caso 7)**: dispara `CallTruco` del nivel más alto disponible
  cuando `botWinsIfRejected && probabilidadDeManoAlta`, **sin verificar el encierro real** (rival
  sin cartas / camino al lock). Si el rival acepta y el bot gana la mano (probabilidad alta por
  construcción), el bot **se pasa de 3 y pierde**. La spec Caso 7 exige escaladar “para dejar al
  rival sin respuesta cuando se quede sin cartas”; la implementación actual no modela ese plan
  multi-turno y se autobusta.
- **Follow-up requerido** (fuera del alcance de T037, que es solo validación + documentación):
    1. Re-gatear la escalada de `LockAndMazoRule` (Caso 7) sobre una condición de encierro real
       (p. ej. `lock.leadsToLockIfAdvance()` / rival sin cartas), no sobre mera aritmética +
       probabilidad de mano.
    2. Re-tunear `ForceRivalBustRule` (umbral 0,70) y verificar que las trampas de envido no
       autobusten al bot cuando `botBustsIfWins(acceptedPointsIfBotWins)`.
    3. Re-correr `BotEngineHeadToHeadBenchmarkTest` (habilitándolo temporalmente) tras cada ajuste
       hasta alcanzar ≥ 60%.
- **Rationale**: documentar honestamente el resultado empírico aunque no cumpla el objetivo es
  parte del contrato de T037 (“documentar la muestra en research.md”). No se falsifica SC-007.
- **Artefactos**: `src/test/java/.../bot/LegacyBotDecisionEngine.java` (adversario),
  `src/test/java/.../commands/BotEngineHeadToHeadBenchmarkTest.java` (benchmark `@Disabled`).
