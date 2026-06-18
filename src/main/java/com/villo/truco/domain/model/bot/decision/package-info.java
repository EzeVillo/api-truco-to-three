/**
 * Motor de decisión del bot: pipeline de reglas tácticas evaluadas por prioridad. Contiene
 * primitivas determinísticas (MatchArithmetic, CardLockAnalyzer), proveedores de probabilidad
 * (TantoProbabilityProvider, UnplayedHandProbability), el contexto unificado (DecisionContext)
 * y el registro de reglas (DecisionRuleRegistry).
 */
package com.villo.truco.domain.model.bot.decision;
