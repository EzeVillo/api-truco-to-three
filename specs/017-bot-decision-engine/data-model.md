# Data Model: Motor de decisión del bot

Fecha: 2026-06-17 · Feature: `017-bot-decision-engine`

Todos los tipos viven en `com.villo.truco.domain.model.bot.decision` (salvo los reutilizados).
Son **value objects inmutables** o **funciones puras**; no hay persistencia.

## DecisionContext (VO de entrada a las reglas)

Empaqueta todo lo que una `DecisionRule` necesita, separando lo determinístico de lo probabilístico.

| Campo          | Tipo                       | Origen                                 | Notas                                               |
|----------------|----------------------------|----------------------------------------|-----------------------------------------------------|
| `view`         | `BotMatchView`             | Capa de aplicación (ACL)               | Mano, marcador, cartas jugadas, cantos disponibles. |
| `arithmetic`   | `MatchArithmetic`          | Derivado de `view.game()`              | Primitivas determinísticas del marcador.            |
| `lock`         | `CardLockAnalyzer`         | Derivado de `view.game()`              | Encierro / legalidad de QYMVAM (determinístico).    |
| `tanto`        | `TantoProbabilityProvider` | Envuelve `EnvidoProbabilityCalculator` | **Solo info oculta**: tanto del rival.              |
| `unplayedHand` | `UnplayedHandProbability`  | Nuevo                                  | **Solo info oculta**: carta alta en mano no jugada. |

**Reglas de validación**: `view` no nulo. Los proveedores de probabilidad se consultan de forma
perezosa; una regla determinística no debe invocarlos.

## DecisionRule (SPI — punto de extensión)

Función pura que representa **una táctica/principio**.

| Miembro    | Firma                                            | Descripción                                                              |
|------------|--------------------------------------------------|--------------------------------------------------------------------------|
| `apply`    | `Optional<BotAction> apply(DecisionContext ctx)` | Devuelve la acción si la táctica aplica; `Optional.empty()` si no opina. |
| `priority` | `int priority()`                                 | Orden de evaluación; menor número = mayor prioridad.                     |
| `name`     | `String name()`                                  | Identificador para logging/trazabilidad de por qué decidió.              |

**Invariantes**:

- Una regla NUNCA devuelve una acción ilegal (p. ej. QYMVAM sin cartas).
- Una regla NUNCA devuelve una jugada de VE negativo (FR-015).
- La última regla del registry (`ExpectedValueFallbackRule`) SIEMPRE devuelve una acción
  (`Optional` no vacío) para garantizar FR-018.

## DecisionRuleRegistry

Lista ordenada e inmutable de `DecisionRule`. `decide(ctx)` recorre por prioridad y devuelve la
primera acción no vacía. Punto único donde se **registran** las tácticas (extensión futura).

| Campo   | Tipo                 | Notas                                   |
|---------|----------------------|-----------------------------------------|
| `rules` | `List<DecisionRule>` | Ordenada por `priority()` al construir. |

### Diagrama de prioridades del registry (menor número = se evalúa antes)

```text
DecisionRuleRegistry.decide(ctx)
  │  recorre por priority() asc; primer Optional no vacío gana
  ▼
  10  ResponseToRivalCallRule   ── Caso 5: QYMVAM / NO_QUIERO que pasa al rival (solo mustRespond)
  20  EnvidoAtTwoTwoRule         ── Caso 1: 2-2 canta envido/falta por prob. del tanto
  30  ForceRivalBustRule         ── Caso 4: envido que obliga al rival a pasarse si gana
  40  LockAndMazoRule            ── Casos 2,3,6,7: encierro sin cartas / escalada / aceptación
 1000  ExpectedValueFallbackRule ── Caso 8: valor esperado (SIEMPRE responde; FR-018)
```

- Las reglas 10–40 son **determinísticas** (no inyectan `Random`); solo consultan `arithmetic`,
  `lock`, `tanto` o `unplayedHand`.
- La regla 1000 es la única que usa `Random` (vía `TrucoDecisionPolicy` / `EnvidoDecisionPolicy` /
  `CardSelectionPolicy`) y **siempre** devuelve una acción → garantiza FR-018.
- Agregar una casuística nueva = insertar una regla con su `priority()` relativa; no se editan las
  existentes (Open/Closed, FR-016).

## MatchArithmetic (primitivas determinísticas)

**Aclaración**: `MatchArithmetic` es **solo un conjunto de primitivas aritméticas puras** sobre el
marcador a 3 — NO es un gate ni una regla de decisión. No decide ninguna jugada; las reglas
(`DecisionRule`) la consultan para evaluar "¿el rival se pasa si acepta/gana/rechaza?" y derivar su
acción. No hay estado, no hay azar, no hay flujo de control de decisión aquí.

Generaliza el actual `TrucoScoreStrategy`. Métodos sobre `(myScore, rivalScore, pointsToWin)` y los
stakes de un canto:

- `rivalBustsIfAccepts(stakeAccepted)` / `rivalBustsIfWins(stakeAccepted)` /
  `rivalBustsIfRejects(stakeRejected)`
- `botBustsIfAccepts(stakeAccepted)` / `botBustsIfWins(stakeAccepted)`
- `botReachesExact(points)` / `rivalReachesExact(points)`
- `rivalWinsMatchIfBotLoses(stakeAccepted)`

Sin estado mutable. Sin azar.

## CardLockAnalyzer (encierro determinístico)

Deriva de `view.game()`: `myCards`, `rivalCardPlayed`, `rivalCardsInHand`, `handsPlayedCount`,
`isMano`.

- `rivalIsOutOfCards()` → `rivalCardsInHand == 0`
- `rivalCannotQYMVAM()` → equivalente a `rivalIsOutOfCards()`
- `botBeatsPlayedCard()` → existe carta propia que mata `rivalCardPlayed` (determinístico, FR-003)
- `botHasGuaranteedTrick()` → mata la última carta jugada del rival estando el rival sin cartas
- `leadsToLockIfAdvance()` → el bot mató la primera y, avanzando, llega con carta y el rival sin
  cartas

## TantoProbabilityProvider (info oculta: tanto del rival)

Fachada fina sobre `EnvidoProbabilityCalculator` + `EnvidoScoring`.

- `probabilityBotWinsTanto()` → `double` en `[0,1]`
- Conveniencias semánticas para reglas: `moreLikelyToLoseTanto()`,
  `moreLikelyToWinTanto()`, `tie()` (soporta el desempate de FR-017).

## UnplayedHandProbability (info oculta: mano no jugada)

Nueva calculadora combinatoria (patrón de `EnvidoProbabilityCalculator`).

- `probabilityHighCardWinsUnplayedTrick()` → `double` en `[0,1]`, enumerando las posibles cartas del
  rival entre el mazo restante (40 menos las conocidas: propias + jugadas).

## Acciones de salida (reutilizadas, sin cambios)

`BotAction` (sealed): `PlayCard`, `CallTruco`, `RespondTruco`, `CallEnvido`, `RespondEnvido`,
`Fold`. Cubren todas las jugadas de la spec, incluida "avanzar sin cantar" (= `PlayCard`) e "irse al
mazo" (= `Fold`/`RespondTruco` según contexto del dominio del match).

## Trazabilidad casuística → regla

| Caso spec               | Regla principal                                 | Fuente de decisión                        |
|-------------------------|-------------------------------------------------|-------------------------------------------|
| 1 (2-2 cantar tanto)    | `EnvidoAtTwoTwoRule`                            | Probabilidad del tanto + aritmética       |
| 2 (encierro sin cartas) | `LockAndMazoRule`                               | Determinística                            |
| 3 (2-1 abajo, mata 1ª)  | `ForceRivalBustRule` → `LockAndMazoRule`        | Aritmética + determinística               |
| 4 (2-1 arriba)          | `ForceRivalBustRule`                            | Probabilidad del tanto + aritmética       |
| 5 (responder canto)     | `ResponseToRivalCallRule`                       | Aritmética                                |
| 6 (1-1, truco)          | `LockAndMazoRule` (+ `UnplayedHandProbability`) | Determinística / prob. mano no jugada     |
| 7 (0-0, vale cuatro)    | `LockAndMazoRule`                               | Determinística                            |
| 8 (regla general VE)    | `ExpectedValueFallbackRule`                     | Valor esperado                            |
| **2-0 y futuras**       | **nueva `DecisionRule`** o reglas existentes    | Aritmética/probabilidad según corresponda |
