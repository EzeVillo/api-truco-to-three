package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Optional;

public final class ResponseToRivalCallRule implements DecisionRule {

  private static final int PRIORITY = 10;

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var truco = ctx.view().truco();
    if (!truco.mustRespond()) {
      return Optional.empty();
    }

    final var pending = truco.currentCall();
    final var arithmetic = ctx.arithmetic();

    if (truco.canRespondWith(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)
        && arithmetic.rivalBustsIfAccepts(pending.stakeIfAccepted())) {
      return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO));
    }

    if (arithmetic.rivalBustsIfRejects(pending.stakeIfRejected())) {
      return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.NO_QUIERO));
    }

    return Optional.empty();
  }

  @Override
  public int priority() {

    return PRIORITY;
  }

  @Override
  public String name() {

    return "ResponseToRivalCallRule";
  }

}
