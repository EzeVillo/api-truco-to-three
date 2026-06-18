package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import java.util.Optional;

public final class EnvidoAtTwoTwoRule implements DecisionRule {

  private static final int PRIORITY = 20;

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var envido = ctx.view().envido();
    final var game = ctx.view().game();

    if (!envido.canCall() || envido.mustRespond()) {
      return Optional.empty();
    }

    final int matchPoint = game.pointsToWin() - 1;
    if (game.myScore() != matchPoint || game.rivalScore() != matchPoint) {
      return Optional.empty();
    }

    // empate 50/50 → "no es más probable perder" → falta envido (D6)
    final var tanto = ctx.tanto();
    final BotEnvidoLevel targetLevel = tanto.moreLikelyToLoseTanto() ? BotEnvidoLevel.ENVIDO
        : BotEnvidoLevel.FALTA_ENVIDO;

    return envido.availableCalls().stream()
        .filter(call -> call.level() == targetLevel)
        .findFirst()
        .or(() -> envido.availableCalls().stream()
            .max(java.util.Comparator.comparingInt(c -> c.level().ordinal())))
        .map(BotAction.CallEnvido::new);
  }

  @Override
  public int priority() {

    return PRIORITY;
  }

  @Override
  public String name() {

    return "EnvidoAtTwoTwoRule";
  }

}
