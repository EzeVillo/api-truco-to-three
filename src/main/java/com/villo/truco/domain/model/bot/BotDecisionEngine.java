package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Objects;
import java.util.Random;

public final class BotDecisionEngine {

  private final TrucoDecisionPolicy trucoPolicy;
  private final EnvidoDecisionPolicy envidoPolicy;
  private final CardSelectionPolicy cardPolicy;

  public BotDecisionEngine(final BotPersonality personality, final EnvidoScoring envidoScoring) {

    this(personality, new Random(), envidoScoring);
  }

  BotDecisionEngine(final BotPersonality personality, final Random random,
      final EnvidoScoring envidoScoring) {

    Objects.requireNonNull(personality);
    Objects.requireNonNull(random);
    Objects.requireNonNull(envidoScoring);
    this.trucoPolicy = new TrucoDecisionPolicy(personality, random);
    this.envidoPolicy = new EnvidoDecisionPolicy(personality, random, envidoScoring);
    this.cardPolicy = new CardSelectionPolicy(personality, random);
  }

  private static boolean isGuaranteedWinningTrucoCall(final BotMatchView.GameContext game,
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

  public BotAction decide(final BotMatchView view) {

    Objects.requireNonNull(view);

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
        return new BotAction.RespondTruco(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
      }

      if (TrucoScoreStrategy.noQuieroKillsRival(rivalScore, pendingCall, pointsToWin)) {
        return new BotAction.RespondTruco(BotTrucoResponse.NO_QUIERO);
      }

      if (envido.canCall()) {
        final var envidoCall = this.envidoPolicy.decideCall(envido.availableCalls(), envidoScore,
            myScore, rivalScore, pointsToWin, game.isMano(), true, game.myCards(),
            game.rivalCardPlayed());
        if (envidoCall.isPresent()) {
          return new BotAction.CallEnvido(envidoCall.get());
        }
      }

      if (!truco.canRespondWith(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)
          && TrucoScoreStrategy.shouldQYMVAM(rivalScore, pendingCall, pointsToWin)) {
        return new BotAction.RespondTruco(BotTrucoResponse.QUIERO);
      }

      if (truco.canCall() && isGuaranteedWinningTrucoCall(game, truco.availableCall(),
          pointsToWin)) {
        return new BotAction.CallTruco(truco.availableCall());
      }

      if (truco.canCall()) {
        final var raise = this.trucoPolicy.decideRaise(truco.availableCall(), handStrength, myScore,
            rivalScore, pointsToWin);
        if (raise.isPresent()) {
          return new BotAction.CallTruco(raise.get());
        }
      }

      final var response = this.trucoPolicy.decideResponse(pendingCall, handStrength, myScore,
          rivalScore, pointsToWin);
      return new BotAction.RespondTruco(response);
    }

    if (envido.mustRespond()) {
      if (envido.canCall()) {
        final var raise = this.envidoPolicy.decideCall(envido.availableCalls(), envidoScore,
            myScore, rivalScore, pointsToWin, game.isMano(), false, game.myCards(),
            game.rivalCardPlayed());
        if (raise.isPresent()) {
          return new BotAction.CallEnvido(raise.get());
        }
      }
      final var response = this.envidoPolicy.decideResponse(envidoScore, myScore, rivalScore,
          pointsToWin, envido.pendingOutcome());
      return new BotAction.RespondEnvido(response);
    }

    if (envido.canCall()) {
      final var envidoCall = this.envidoPolicy.decideCall(envido.availableCalls(), envidoScore,
          myScore, rivalScore, pointsToWin, game.isMano(), true, game.myCards(),
          game.rivalCardPlayed());
      if (envidoCall.isPresent()) {
        return new BotAction.CallEnvido(envidoCall.get());
      }
    }

    if (truco.canCall() && isGuaranteedWinningTrucoCall(game, truco.availableCall(), pointsToWin)) {
      return new BotAction.CallTruco(truco.availableCall());
    }

    if (truco.canCall()) {
      final var trucoCall = this.trucoPolicy.decideCall(truco.availableCall(), handStrength,
          myScore, rivalScore, pointsToWin);
      if (trucoCall.isPresent()) {
        return new BotAction.CallTruco(trucoCall.get());
      }
    }

    if (game.canFold() && game.foldWouldGiveGameToBot()) {
      return new BotAction.Fold();
    }

    final var card = this.cardPolicy.select(game.myCards(), game.rivalCardPlayed());
    return new BotAction.PlayCard(card);
  }

}
