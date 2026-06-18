package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import java.util.Optional;

public interface DecisionRule {

  /**
   * Devuelve la acción del bot si esta táctica aplica a la posición; {@link Optional#empty()} si
   * no opina (cede a la siguiente regla con mayor número de prioridad).
   */
  Optional<BotAction> apply(DecisionContext ctx);

  /** Menor número = mayor prioridad (se evalúa antes). */
  int priority();

  /** Nombre de la regla para logging/trazabilidad. */
  String name();

}
