# Contrato interno: SPI de `DecisionRule`

Fecha: 2026-06-17 · Feature: `017-bot-decision-engine`

El motor del bot no expone interfaces externas (REST/WebSocket) nuevas. El "contrato" relevante es
el **punto de extensión interno** que habilita agregar casuísticas futuras (2-0 ganando/perdiendo y
otros resultados) sin modificar la lógica existente. Este documento lo fija.

## Contrato del motor (sin cambios para la capa de aplicación)

```text
BotDecisionEngine.decide(BotMatchView view) -> BotAction
```

- **Precondición**: `view` describe una posición con al menos una acción legal disponible.
- **Postcondición**: devuelve exactamente una `BotAction` **legal** en esa posición.
- **Garantía**: nunca devuelve QYMVAM sin cartas (FR-006); nunca una jugada de VE negativo (FR-015);
  siempre devuelve una acción (FR-018).

La capa de aplicación (`ExecuteBotTurnCommandHandler`, `AdvanceBotVsBotMatchCommandHandler`) sigue
invocando `decide(view)` igual que hoy.

## Contrato del SPI `DecisionRule`

```text
interface DecisionRule {
    Optional<BotAction> apply(DecisionContext ctx);  // empty = "no opino"
    int priority();                                   // menor = se evalúa antes
    String name();                                    // trazabilidad
}
```

### Reglas del contrato (obligatorias para toda implementación)

1. **Pureza**: `apply` no muta estado ni el contexto; misma entrada ⇒ misma salida, salvo el azar
   explícito de los proveedores de probabilidad inyectados.
2. **Legalidad**: si devuelve una acción, debe ser legal en `ctx.view()` (en particular, QYMVAM solo
   si `ctx.lock().rivalCannotQYMVAM()` no aplica al propio bot y el bot tiene cartas).
3. **No-VE-negativo**: no devolver una acción cuyo resultado esperado sea perder (FR-015).
4. **Determinismo declarado**: una regla determinística NO consulta `ctx.tanto()` ni
   `ctx.unplayedHand()`. Solo las reglas que dependen de información oculta lo hacen (SC-005).
5. **Abstención**: si la táctica no aplica a la posición, devolver `Optional.empty()` para ceder a
   la
   siguiente regla.

### Resolución del pipeline

```text
DecisionRuleRegistry.decide(ctx):
    para cada regla ordenada por priority() asc:
        resultado = regla.apply(ctx)
        si resultado.isPresent(): return resultado.get()
    # nunca se alcanza: la última regla (ExpectedValueFallbackRule) siempre responde
```

## Cómo se agrega una casuística (contrato de extensión)

Para sumar, por ejemplo, el manejo de **2-0 ganando** u otro resultado:

1. Evaluar si las reglas existentes ya lo cubren por aritmética (preferido — FR-016). Si es así,
   agregar solo tests que lo verifiquen.
2. Si requiere una táctica nueva, crear `class FooRule implements DecisionRule` en
   `domain/model/bot/decision/rules/`.
3. Registrarla en `DecisionRuleRegistry` con su `priority()` relativo a las demás.
4. **No** se edita ninguna regla existente ni `BotDecisionEngine` (Open/Closed).
5. Agregar tests unitarios de la regla y, si es un caso destacado, un escenario en
   `BotDecisionEngineTest`.

## Compatibilidad

- `BotMatchView`, `BotAction`, `BotEnvidoCall`, `BotTrucoCall` se mantienen como contrato de entrada
  y salida. Si una regla necesita un dato adicional ya conocido por el dominio (p. ej. detalle de
  cartas jugadas por mano), se extiende `BotMatchView` y su ACL (`MatchToBotACL`) de forma aditiva,
  actualizando los call sites explícitamente (sin overloads de conveniencia).
