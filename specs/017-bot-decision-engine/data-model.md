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

## MatchArithmetic (primitivas determinísticas)

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
