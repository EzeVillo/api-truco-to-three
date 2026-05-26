package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Optional;
import java.util.Random;

final class TrucoDecisionPolicy {

  private static final double CALL_BASE = 0.25;
  private static final double RESPOND_ACCEPT_BASE = 0.35;

  private final BotPersonality personality;
  private final Random random;

  TrucoDecisionPolicy(final BotPersonality personality, final Random random) {

    this.personality = personality;
    this.random = random;
  }

  Optional<BotTrucoCall> decideCall(final BotTrucoCall availableCall, final double handStrength,
      final int myScore, final int rivalScore, final int pointsToWin) {

    if (availableCall == null) {
      return Optional.empty();
    }

    if (myScore + availableCall.stakeIfAccepted() > pointsToWin) {
      return Optional.empty();
    }

    final var botWinsIfRejected = TrucoScoreStrategy.botWinsIfRejected(myScore, availableCall,
        pointsToWin);
    final var rivalExceedsIfAccepted = TrucoScoreStrategy.rivalExceedsIfAccepted(rivalScore,
        availableCall, pointsToWin);

    if (botWinsIfRejected) {
      return Optional.of(availableCall);
    }

    if (rivalExceedsIfAccepted) {
      return Optional.of(availableCall);
    }

    final var bluffBoost = this.personality.mentiroso() / 300.0;
    final var temerarioFactor = this.personality.temerario() / 100.0;
    final var callProbability = CALL_BASE + temerarioFactor * handStrength + bluffBoost;

    if (this.random.nextDouble() > callProbability) {
      return Optional.empty();
    }

    return Optional.of(availableCall);
  }

  Optional<BotTrucoCall> decideRaise(final BotTrucoCall availableRaise, final double handStrength,
      final int myScore, final int rivalScore, final int pointsToWin) {

    if (availableRaise == null) {
      return Optional.empty();
    }

    if (myScore + availableRaise.stakeIfAccepted() > pointsToWin) {
      return Optional.empty();
    }

    final var botWinsIfRejected = TrucoScoreStrategy.botWinsIfRejected(myScore, availableRaise,
        pointsToWin);
    final var rivalExceedsIfAccepted = TrucoScoreStrategy.rivalExceedsIfAccepted(rivalScore,
        availableRaise, pointsToWin);

    if (botWinsIfRejected) {
      return Optional.of(availableRaise);
    }

    if (rivalExceedsIfAccepted) {
      return Optional.of(availableRaise);
    }

    final double temerarioFactor = this.personality.temerario() / 100.0;
    final double raiseProbability =
        temerarioFactor * handStrength * 0.5 + this.personality.mentiroso() / 200.0;

    if (this.random.nextDouble() < raiseProbability) {
      return Optional.of(availableRaise);
    }

    return Optional.empty();
  }

  BotTrucoResponse decideResponse(final BotTrucoCall pendingOffer, final double handStrength,
      final int myScore, final int rivalScore, final int pointsToWin) {

    if (TrucoScoreStrategy.rivalWinsIfRejected(rivalScore, pendingOffer, pointsToWin)
        && !TrucoScoreStrategy.botExceedsIfAccepted(myScore, pendingOffer, pointsToWin)) {
      return BotTrucoResponse.QUIERO;
    }

    if (TrucoScoreStrategy.botExceedsIfAccepted(myScore, pendingOffer, pointsToWin)) {
      return BotTrucoResponse.NO_QUIERO;
    }

    final var temerarioFactor = this.personality.temerario() / 100.0;
    final var acceptProbability = RESPOND_ACCEPT_BASE + temerarioFactor * handStrength;

    if (this.random.nextDouble() < acceptProbability) {
      return BotTrucoResponse.QUIERO;
    }
    return BotTrucoResponse.NO_QUIERO;
  }

}
