# Implementation Plan: Motor de decisión del bot por aritmética del match a 3

**Branch**: `017-bot-decision-engine` | **Date**: 2026-06-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/017-bot-decision-engine/spec.md`

## Summary

Rediseñar la toma de decisiones del bot (`BotDecisionEngine` y sus policies) para que cada canto,
respuesta y jugada se deduzca de la **aritmética del marcador a 3** y de la **posición de cartas**
(determinístico), recurriendo a probabilidad **solo** para información oculta (tanto del rival y
manos no jugadas).

El diseño se organiza como un **pipeline de tácticas (`DecisionRule`)** evaluadas en orden de
prioridad sobre primitivas aritméticas puras. Cada táctica encapsula un *principio* (forzar que el
rival se pase, encierro sin cartas, llegar exacto a 3, valor esperado) — no un marcador literal. El
objetivo explícito del usuario es que **agregar casuísticas futuras** (p. ej. 2-0 ganando/perdiendo
u otros resultados) sea un cambio aditivo: registrar una nueva `DecisionRule` o dejar que la
aritmética de las reglas existentes la absorba, sin reescribir las decisiones ya implementadas.

## Technical Context

**Language/Version**: Java 21 (records, sealed interfaces, record patterns en `switch`)

**Primary Dependencies**: Ninguna nueva. Dominio Java puro (cero Spring). Reutiliza
`EnvidoProbabilityCalculator` y `EnvidoScoring` existentes.

**Storage**: N/A (lógica de decisión en memoria; sin persistencia nueva)

**Testing**: JUnit 5 (tests de dominio puros, sin Spring ni Docker, vía Gradle). Determinismo de las
ramas no aleatorias se prueba sin `Random`.

**Target Platform**: Backend Spring Boot (servidor); el código nuevo vive en `domain`.

**Project Type**: Web service (backend Java) — la feature toca solo la capa de dominio del bot.

**Performance Goals**: Decisión del bot < 50 ms por turno. La enumeración probabilística
(combinatoria sobre ~37 cartas restantes) ya está acotada y se mantiene.

**Constraints**: Dominio puro (Principio II), arquitectura unidireccional `Domain → Application →
Infrastructure` reforzada por ArchUnit, cobertura JaCoCo ≥ 70%.

**Scale/Scope**: Reescritura acotada del paquete `com.villo.truco.domain.model.bot`. Sin cambios de
API REST, WebSocket, esquema de datos ni UI.

## Constitution Check

*GATE: Debe pasar antes de Phase 0. Re-evaluado tras Phase 1.*

| Principio              | Estado        | Notas                                                                                                                                                                                                                                       |
|------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| I. Hexagonal + DDD     | ✅ PASA        | Todo el cambio vive en `domain.model.bot`. No cruza capas; no se importan otros agregados.                                                                                                                                                  |
| II. Dominio puro       | ✅ PASA        | Java estándar, sin Spring/JPA. La aleatoriedad sigue inyectándose vía `Random`.                                                                                                                                                             |
| III. Test-First + ≥70% | ✅ PASA        | Cada táctica y primitiva aritmética se cubre con tests de dominio en español, manejados por `BotDecisionEngineTest` y nuevas clases.                                                                                                        |
| IV. Español            | ✅ PASA        | Todos los artefactos y títulos de tests en español. Código en inglés según convención.                                                                                                                                                      |
| V. Simplicidad / YAGNI | ⚠ JUSTIFICADO | El pipeline de `DecisionRule` introduce una abstracción por encima de un único método. Ver Complexity Tracking: la extensibilidad es un **requisito explícito del usuario** (casuísticas 2-0 y otras) y el FR-016, no un futuro hipotético. |

**Resultado**: PASA con una desviación justificada de YAGNI (documentada abajo).

## Project Structure

### Documentation (this feature)

```text
specs/017-bot-decision-engine/
├── plan.md              # Este archivo
├── research.md          # Phase 0 — decisiones de diseño
├── data-model.md        # Phase 1 — entidades/VOs del motor
├── quickstart.md        # Phase 1 — cómo agregar una casuística nueva
├── contracts/
│   └── decision-rule-spi.md   # Contrato interno del punto de extensión
├── checklists/
│   └── requirements.md  # Generado por /speckit-specify
└── tasks.md             # Generado por /speckit-tasks (no por este comando)
```

### Source Code (repository root)

```text
src/main/java/com/villo/truco/domain/model/bot/
├── BotDecisionEngine.java          # Orquesta el pipeline (reescrito: arma y corre las reglas)
├── decision/                       # NUEVO subpaquete — núcleo del rediseño
│   ├── DecisionRule.java           # SPI: (DecisionContext) -> Optional<BotAction> + priority()
│   ├── DecisionContext.java        # VO inmutable: BotMatchView + arithmetic + probabilidad
│   ├── DecisionRuleRegistry.java   # Lista ordenada de reglas; primer match gana
│   ├── MatchArithmetic.java        # Primitivas determinísticas del marcador a 3
│   ├── CardLockAnalyzer.java       # Detección determinística de encierro (rival sin cartas / QYMVAM ilegal)
│   ├── TantoProbabilityProvider.java   # Envuelve EnvidoProbabilityCalculator (info oculta: tanto)
│   ├── UnplayedHandProbability.java    # Probabilidad de mano no jugada (info oculta: carta alta)
│   └── rules/                      # Una clase por táctica/principio
│       ├── ForceRivalBustRule.java       # Casos 1,4,5 (lado "forzar que se pase")
│       ├── EnvidoAtTwoTwoRule.java        # Caso 1 (2-2 cantar el tanto)
│       ├── LockAndMazoRule.java           # Casos 2,3,6,7 (encierro sin cartas)
│       ├── ResponseToRivalCallRule.java   # Caso 5 (no quiero / QYMVAM que pasa al rival)
│       └── ExpectedValueFallbackRule.java # Caso 8 (VE cuando nada fuerza)
├── EnvidoProbabilityCalculator.java   # Reutilizado
├── EnvidoScoring.java                 # Reutilizado
├── HandStrengthEvaluator.java         # Reutilizado por la regla de VE
├── CardSelectionPolicy.java           # Reutilizado por jugadas de carta
└── valueobjects/                      # BotMatchView, BotAction, etc. (se extienden si hace falta)

src/test/java/com/villo/truco/domain/model/bot/
├── BotDecisionEngineTest.java         # Tests de integración del pipeline (8 casos)
└── decision/                          # Tests unitarios por regla y por primitiva aritmética
```

**Structure Decision**: Single project (backend Java). El rediseño se concentra en un subpaquete
nuevo `domain.model.bot.decision`, dejando intactos los value objects y reutilizando las
calculadoras de probabilidad existentes. `BotDecisionEngine` pasa de orquestar policies
probabilísticas a orquestar el pipeline de reglas.

## Complexity Tracking

| Violación                                                                                           | Por qué se necesita                                                                                                                                                                                                                                                            | Alternativa más simple, y por qué se rechaza                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Pipeline `DecisionRule` (registry de tácticas) en vez de un único método `decide()` con `if`/`else` | El usuario pidió **explícitamente** que el motor sea extensible para sumar casuísticas (2-0 ganando/perdiendo y otros resultados); la spec lista 8 casos y anticipa más (FR-016). Reglas separadas por principio dan puntos de extensión claros y testeables de forma aislada. | Un solo método con condicionales: rechazado porque cada casuística nueva obligaría a editar y re-testear la lógica ya estable, creciendo a un método inmantenible — exactamente el "cableado caso por caso" que el FR-016 prohíbe. |
| Subpaquete `decision/` con varias clases pequeñas                                                   | Aísla primitivas aritméticas (determinísticas) de proveedores de probabilidad (info oculta), haciendo verificable el límite del SC-005.                                                                                                                                        | Mantener todo en las policies actuales: rechazado porque mezcla azar y aritmética en los mismos métodos, lo que hace imposible garantizar el SC-005 ("probabilidad solo para info oculta").                                        |

> Nota YAGNI: el pipeline **no** anticipa requisitos hipotéticos — implementa un requisito explícito
> ya enunciado. Las reglas se agrupan por *principio aritmético*, no por marcador literal, de modo
> que muchas casuísticas futuras (incluido 2-0) quedan absorbidas por las reglas existentes sin
> agregar código.
