package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import com.villo.truco.domain.model.match.MatchPlayerDecisionView;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.ArrayList;
import java.util.List;

final class MatchToBotACL {

  private MatchToBotACL() {

  }

  static BotMatchView translate(final MatchPlayerDecisionView view) {

    final var game = view.game();
    final var truco = view.truco();
    final var envido = view.envido();

    final var gameCtx = new GameContext(game.myCards().stream().map(MatchToBotACL::toBot).toList(),
        game.myScore(), game.rivalScore(),
        game.rivalCardPlayed() != null ? toBot(game.rivalCardPlayed()) : null, game.envidoScore(),
        game.handsPlayedCount(), game.isMano(), game.canPlayCard(), game.canFold(),
        game.foldWouldGiveGameToBot(), game.pointsToWin());

    final var trucoCtx = new TrucoContext(
        truco.availableCall() != null ? toBot(truco.availableCall()) : null,
        truco.availableResponses().stream().map(MatchToBotACL::toBot).toList(),
        truco.currentCall() != null ? toBot(truco.currentCall()) : null);

    final var envidoCtx = new EnvidoContext(
        envido.availableCalls().stream().map(option -> toBot(option, envido.currentChain()))
            .toList(), envido.availableResponses().stream().map(MatchToBotACL::toBot).toList(),
        envido.currentChain().stream().map(option -> toBot(option, List.of())).toList(),
        envido.pendingOutcome() != null ? toBot(envido.pendingOutcome()) : null);

    return new BotMatchView(gameCtx, trucoCtx, envidoCtx);
  }

  static Card toCard(final BotCard botCard) {

    return botCard.card();
  }

  static TrucoResponse toTrucoResponse(final BotTrucoResponse response) {

    return switch (response) {
      case QUIERO -> TrucoResponse.QUIERO;
      case NO_QUIERO -> TrucoResponse.NO_QUIERO;
      case QUIERO_Y_ME_VOY_AL_MAZO -> TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO;
    };
  }

  static EnvidoCall toEnvidoCall(final BotEnvidoCall call) {

    return switch (call.level()) {
      case ENVIDO -> EnvidoCall.ENVIDO;
      case REAL_ENVIDO -> EnvidoCall.REAL_ENVIDO;
      case FALTA_ENVIDO -> EnvidoCall.FALTA_ENVIDO;
    };
  }

  static EnvidoResponse toEnvidoResponse(final BotEnvidoResponse answer) {

    return switch (answer) {
      case QUIERO -> EnvidoResponse.QUIERO;
      case NO_QUIERO -> EnvidoResponse.NO_QUIERO;
    };
  }

  private static BotCard toBot(final MatchPlayerDecisionView.CardView card) {

    return new BotCard(card.trucoValue(), card.card());
  }

  private static BotTrucoCall toBot(final TrucoCall call) {

    return new BotTrucoCall(call.pointsIfAccepted(), call.pointsIfRejected());
  }

  private static BotTrucoResponse toBot(final TrucoResponse response) {

    return switch (response) {
      case QUIERO -> BotTrucoResponse.QUIERO;
      case NO_QUIERO -> BotTrucoResponse.NO_QUIERO;
      case QUIERO_Y_ME_VOY_AL_MAZO -> BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO;
    };
  }

  private static BotEnvidoCall toBot(final MatchPlayerDecisionView.EnvidoOption option,
      final List<MatchPlayerDecisionView.EnvidoOption> currentChain) {

    final var level = switch (option.call()) {
      case ENVIDO -> BotEnvidoLevel.ENVIDO;
      case REAL_ENVIDO -> BotEnvidoLevel.REAL_ENVIDO;
      case FALTA_ENVIDO -> BotEnvidoLevel.FALTA_ENVIDO;
    };
    final var projectedChain = new ArrayList<>(currentChain);
    projectedChain.add(option);
    return new BotEnvidoCall(acceptedPointsIfBotWins(projectedChain),
        acceptedPointsIfRivalWins(projectedChain), rejectedPoints(projectedChain), level);
  }

  private static BotMatchView.PendingEnvidoOutcome toBot(
      final MatchPlayerDecisionView.PendingEnvidoOutcome outcome) {

    return new BotMatchView.PendingEnvidoOutcome(outcome.acceptedPointsIfPlayerWins(),
        outcome.acceptedPointsIfRivalWins(), outcome.rejectedPoints());
  }

  private static BotEnvidoResponse toBot(final EnvidoResponse response) {

    return switch (response) {
      case QUIERO -> BotEnvidoResponse.QUIERO;
      case NO_QUIERO -> BotEnvidoResponse.NO_QUIERO;
    };
  }

  private static int acceptedPointsIfBotWins(
      final List<MatchPlayerDecisionView.EnvidoOption> projectedChain) {

    return projectedChain.stream().filter(option -> option.call() == EnvidoCall.FALTA_ENVIDO)
        .findFirst().map(MatchPlayerDecisionView.EnvidoOption::pointsIfPlayerWins).orElseGet(
            () -> projectedChain.stream()
                .mapToInt(MatchPlayerDecisionView.EnvidoOption::pointsIfPlayerWins).sum());
  }

  private static int acceptedPointsIfRivalWins(
      final List<MatchPlayerDecisionView.EnvidoOption> projectedChain) {

    return projectedChain.stream().filter(option -> option.call() == EnvidoCall.FALTA_ENVIDO)
        .findFirst().map(MatchPlayerDecisionView.EnvidoOption::pointsIfRivalWins).orElseGet(
            () -> projectedChain.stream()
                .mapToInt(MatchPlayerDecisionView.EnvidoOption::pointsIfRivalWins).sum());
  }

  private static int rejectedPoints(
      final List<MatchPlayerDecisionView.EnvidoOption> projectedChain) {

    if (projectedChain.size() == 1) {
      return 1;
    }

    return projectedChain.subList(0, projectedChain.size() - 1).stream()
        .filter(option -> option.call() != EnvidoCall.FALTA_ENVIDO)
        .mapToInt(MatchPlayerDecisionView.EnvidoOption::pointsIfPlayerWins).sum();
  }

}
