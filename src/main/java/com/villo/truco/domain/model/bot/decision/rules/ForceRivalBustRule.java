package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import java.util.Optional;

public final class ForceRivalBustRule implements DecisionRule {

  static final double HIGH_LOSS_PROBABILITY_THRESHOLD = 0.70;
  private static final int PRIORITY = 30;

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var envido = ctx.view().envido();
    if (!envido.canCall() || envido.mustRespond()) {
      return Optional.empty();
    }

    final double lossProbability = 1.0 - ctx.tanto().probabilityBotWinsTanto();
    if (lossProbability <= HIGH_LOSS_PROBABILITY_THRESHOLD) {
      return Optional.empty();
    }

    final var arithmetic = ctx.arithmetic();
    return envido.availableCalls().stream()
        .filter(call -> arithmetic.rivalBustsIfWins(call.acceptedPointsIfRivalWins()))
        .max(java.util.Comparator.comparingInt(c -> c.level().ordinal()))
        .map(BotAction.CallEnvido::new);
  }

  @Override
  public int priority() {

    return PRIORITY;
  }

  @Override
  public String name() {

    return "ForceRivalBustRule";
  }

}
