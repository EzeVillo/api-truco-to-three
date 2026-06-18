package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.CardSelectionPolicy;
import com.villo.truco.domain.model.bot.EnvidoDecisionPolicy;
import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.HandStrengthEvaluator;
import com.villo.truco.domain.model.bot.TrucoDecisionPolicy;
import com.villo.truco.domain.model.bot.TrucoScoreStrategy;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Optional;
import java.util.Random;

public final class ExpectedValueFallbackRule implements DecisionRule {

  private static final int PRIORITY = 1000;

  private final TrucoDecisionPolicy trucoPolicy;
  private final EnvidoDecisionPolicy envidoPolicy;
  private final CardSelectionPolicy cardPolicy;

  public ExpectedValueFallbackRule(final BotPersonality personality, final Random random,
      final EnvidoScoring envidoScoring) {

    this.trucoPolicy = new TrucoDecisionPolicy(personality, random);
    this.envidoPolicy = new EnvidoDecisionPolicy(personality, random, envidoScoring);
    this.cardPolicy = new CardSelectionPolicy(personality, random);
  }

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var view = ctx.view();
    final var game = view.game();
    final var truco = view.truco();
    final var envido = view.envido();

    final var handStrength = HandStrengthEvaluator.contextualStrength(game.myCards(),
        game.rivalCardPlayed(), game.handsPlayedCount());
    final int envidoScore = game.envidoScore();
    final int myScore = game.myScore();
    final int rivalScore = game.rivalScore();
    final int pointsToWin = game.pointsToWin();

    if (truco.mustRespond()) {
      final var pendingCall = truco.currentCall();

      if (truco.canRespondWith(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)
          && TrucoScoreStrategy.shouldQYMVAM(rivalScore, pendingCall, pointsToWin)) {
        return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO));
      }

      if (TrucoScoreStrategy.noQuieroKillsRival(rivalScore, pendingCall, pointsToWin)) {
        return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.NO_QUIERO));
      }

      if (envido.canCall()) {
        final var envidoCall = envidoPolicy.decideCall(envido.availableCalls(), envidoScore,
            myScore, rivalScore, pointsToWin, game.isMano(), true, game.myCards(),
            game.rivalCardPlayed());
        if (envidoCall.isPresent()) {
          return Optional.of(new BotAction.CallEnvido(envidoCall.get()));
        }
      }

      if (!truco.canRespondWith(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)
          && TrucoScoreStrategy.shouldQYMVAM(rivalScore, pendingCall, pointsToWin)) {
        return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.QUIERO));
      }

      if (truco.canCall() && isGuaranteedWinningTrucoCall(game, truco.availableCall(),
          pointsToWin)) {
        return Optional.of(new BotAction.CallTruco(truco.availableCall()));
      }

      if (truco.canCall()) {
        final var raise = trucoPolicy.decideRaise(truco.availableCall(), handStrength, myScore,
            rivalScore, pointsToWin);
        if (raise.isPresent()) {
          return Optional.of(new BotAction.CallTruco(raise.get()));
        }
      }

      final var response = trucoPolicy.decideResponse(pendingCall, handStrength, myScore,
          rivalScore, pointsToWin);
      return Optional.of(new BotAction.RespondTruco(response));
    }

    if (envido.mustRespond()) {
      if (envido.canCall()) {
        final var raise = envidoPolicy.decideCall(envido.availableCalls(), envidoScore, myScore,
            rivalScore, pointsToWin, game.isMano(), false, game.myCards(), game.rivalCardPlayed());
        if (raise.isPresent()) {
          return Optional.of(new BotAction.CallEnvido(raise.get()));
        }
      }
      final var response = envidoPolicy.decideResponse(envidoScore, myScore, rivalScore,
          pointsToWin, envido.pendingOutcome());
      return Optional.of(new BotAction.RespondEnvido(response));
    }

    if (envido.canCall()) {
      final var envidoCall = envidoPolicy.decideCall(envido.availableCalls(), envidoScore, myScore,
          rivalScore, pointsToWin, game.isMano(), true, game.myCards(), game.rivalCardPlayed());
      if (envidoCall.isPresent()) {
        return Optional.of(new BotAction.CallEnvido(envidoCall.get()));
      }
    }

    if (truco.canCall() && isGuaranteedWinningTrucoCall(game, truco.availableCall(), pointsToWin)) {
      return Optional.of(new BotAction.CallTruco(truco.availableCall()));
    }

    if (truco.canCall()) {
      final var trucoCall = trucoPolicy.decideCall(truco.availableCall(), handStrength, myScore,
          rivalScore, pointsToWin);
      if (trucoCall.isPresent()) {
        return Optional.of(new BotAction.CallTruco(trucoCall.get()));
      }
    }

    if (game.canFold() && game.foldWouldGiveGameToBot()) {
      return Optional.of(new BotAction.Fold());
    }

    final var card = cardPolicy.select(game.myCards(), game.rivalCardPlayed());
    return Optional.of(new BotAction.PlayCard(card));
  }

  @Override
  public int priority() {

    return PRIORITY;
  }

  @Override
  public String name() {

    return "ExpectedValueFallbackRule";
  }

  private static boolean isGuaranteedWinningTrucoCall(
      final com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext game,
      final BotTrucoCall availableCall, final int pointsToWin) {

    if (game.rivalCardPlayed() == null || game.rivalCardsInHand() > 0) {
      return false;
    }
    final var rivalRank = game.rivalCardPlayed().trucoRank();
    final var canBeat = game.myCards().stream().anyMatch(card -> card.trucoRank() > rivalRank);
    if (canBeat) {
      return false;
    }
    return TrucoScoreStrategy.botWinsIfRejected(game.myScore(), availableCall, pointsToWin)
        && TrucoScoreStrategy.rivalExceedsIfAccepted(game.rivalScore(), availableCall, pointsToWin);
  }

}
