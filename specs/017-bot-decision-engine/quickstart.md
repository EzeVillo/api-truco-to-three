# Quickstart: trabajar con el motor de decisión del bot

Fecha: 2026-06-17 · Feature: `017-bot-decision-engine`

## Dónde vive

```text
src/main/java/com/villo/truco/domain/model/bot/decision/
```

- `BotDecisionEngine` (paquete padre) orquesta: arma `DecisionContext` y corre el
  `DecisionRuleRegistry`.
- `rules/` contiene una clase por táctica (principio aritmético).

## Cómo decide el bot (resumen)

1. La capa de aplicación llama `engine.decide(view)` con un `BotMatchView`.
2. El motor construye `DecisionContext` = `view` + `MatchArithmetic` + `CardLockAnalyzer` +
   proveedores de probabilidad.
3. El `DecisionRuleRegistry` evalúa las reglas por prioridad; la primera que **opina** gana.
4. Si ninguna táctica forzada aplica, `ExpectedValueFallbackRule` decide por valor esperado.

## Agregar una casuística nueva (p. ej. 2-0 ganando/perdiendo)

1. **Verificá primero si ya está cubierta** por la aritmética de las reglas existentes
   (`ForceRivalBustRule`, `LockAndMazoRule`, etc.). Si lo está, agregá solo un test que lo afirme.
2. Si necesita una táctica nueva:
    - Creá `rules/MiCasuisticaRule.java` implementando `DecisionRule`.
    - Mantené `apply` puro; consultá `ctx.tanto()` / `ctx.unplayedHand()` **solo** si depende de
      información oculta.
    - Devolvé `Optional.empty()` cuando la táctica no aplique.
3. Registrala en `DecisionRuleRegistry` con su `priority()`.
4. **No edites** reglas existentes ni `BotDecisionEngine`.
5. Agregá tests (ver abajo).

## Tests

```bash
# Solo el motor del bot
./gradlew test --tests "com.villo.truco.domain.model.bot.*"

# Suite completa con verificación de cobertura (≥ 70%)
./gradlew build
```

- Tests de **primitivas** (`MatchArithmetic`, `CardLockAnalyzer`): tablas de marcador/cartas, sin
  `Random`.
- Tests de **regla**: armá un `DecisionContext` a mano y afirmá la `BotAction`.
- Tests de **pipeline** (`BotDecisionEngineTest`): reproducen los 8 casos de la spec.
- Las ramas determinísticas se prueban **sin** aleatoriedad para garantizar repetibilidad (SC-005).

## Verificación rápida de los principios de la spec

| Querés verificar…                    | Mirá / corré                                                      |
|--------------------------------------|-------------------------------------------------------------------|
| Que el bot fuerza al rival a pasarse | tests de `ForceRivalBustRule` y `ResponseToRivalCallRule`         |
| Encierro sin cartas                  | tests de `LockAndMazoRule` + `CardLockAnalyzer`                   |
| 2-2 cantar el tanto                  | test de `EnvidoAtTwoTwoRule` (envido vs falta según probabilidad) |
| Que no usa azar de más               | una regla determinística no inyecta proveedores de probabilidad   |
| Que nunca elige QYMVAM sin cartas    | test negativo en `CardLockAnalyzer` / pipeline                    |

## Convenciones del proyecto a respetar

- Dominio puro: sin Spring/JPA en `domain` (ArchUnit lo verifica).
- `import` statements, no FQCN inline.
- Si cambia la firma del constructor del motor, actualizá los call sites explícitamente
  (`ExecuteBotTurnCommandHandler`, `AdvanceBotVsBotMatchCommandHandler`, `BotConfiguration`) — sin
  overloads de conveniencia.
- Títulos de tests en español.
