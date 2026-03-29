package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.ActionType;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

final class MatchPlayerDecisionViewExtractor {

  private MatchPlayerDecisionViewExtractor() {

  }

  static MatchPlayerDecisionView extract(final Match match, final PlayerId playerId) {

    final var isPlayerOne = playerId.equals(match.getPlayerOne());
    final var myScore = isPlayerOne ? match.getScorePlayerOne() : match.getScorePlayerTwo();
    final var rivalScore = isPlayerOne ? match.getScorePlayerTwo() : match.getScorePlayerOne();

    if (match.getCurrentRound() == null) {
      return MatchPlayerDecisionView.empty(myScore, rivalScore);
    }

    final var availableActions = match.getAvailableActions(playerId);
    final var currentHandInfo = match.getCurrentHandInfo();
    final var rivalCardRaw =
        isPlayerOne ? currentHandInfo.cardPlayerTwo() : currentHandInfo.cardPlayerOne();
    final var isMano = currentHandInfo.mano() != null && currentHandInfo.mano().equals(playerId);
    final var myCardsRaw = match.getCardsOf(playerId);

    final var gameContext = new MatchPlayerDecisionView.GameContext(
        myCardsRaw.stream().map(MatchPlayerDecisionViewExtractor::toCardView).toList(), myScore,
        rivalScore, rivalCardRaw != null ? toCardView(rivalCardRaw) : null,
        myCardsRaw.isEmpty() ? 0 : CardEvaluationService.envidoScore(myCardsRaw),
        match.getPlayedHands().size(), isMano, hasAction(availableActions, ActionType.PLAY_CARD),
        hasAction(availableActions, ActionType.FOLD),
        foldWouldGiveGameToBot(match, rivalScore, availableActions),
        ScoringPolicy.pointsToWinGame());

    final var trucoContext = new MatchPlayerDecisionView.TrucoContext(
        toSingleAvailableTrucoCall(availableActions),
        availableActions.stream().filter(a -> a.type() == ActionType.RESPOND_TRUCO)
            .flatMap(a -> a.getParameter().stream())
            .map(value -> tryParse(value, TrucoResponse.class)).filter(Objects::nonNull).distinct()
            .toList(), match.getCurrentTrucoCall());

    final var currentEnvidoChain = match.getEnvidoChain().stream()
        .map(call -> toEnvidoOption(call, myScore, rivalScore, ScoringPolicy.pointsToWinGame()))
        .toList();

    final var envidoContext = new MatchPlayerDecisionView.EnvidoContext(
        availableActions.stream().filter(a -> a.type() == ActionType.CALL_ENVIDO)
            .flatMap(a -> a.getParameter().stream()).map(value -> tryParse(value, EnvidoCall.class))
            .filter(Objects::nonNull).distinct()
            .map(call -> toEnvidoOption(call, myScore, rivalScore, ScoringPolicy.pointsToWinGame()))
            .toList(), availableActions.stream().filter(a -> a.type() == ActionType.RESPOND_ENVIDO)
        .flatMap(a -> a.getParameter().stream()).map(value -> tryParse(value, EnvidoResponse.class))
        .filter(Objects::nonNull).distinct().toList(), currentEnvidoChain,
        toPendingEnvidoOutcome(currentEnvidoChain));

    return new MatchPlayerDecisionView(gameContext, trucoContext, envidoContext);
  }

  private static MatchPlayerDecisionView.CardView toCardView(final Card card) {

    return new MatchPlayerDecisionView.CardView(CardEvaluationService.trucoValue(card), card);
  }

  private static TrucoCall toSingleAvailableTrucoCall(
      final List<AvailableAction> availableActions) {

    final var availableCalls = availableActions.stream()
        .filter(a -> a.type() == ActionType.CALL_TRUCO).flatMap(a -> a.getParameter().stream())
        .map(value -> tryParse(value, TrucoCall.class)).filter(Objects::nonNull).distinct()
        .toList();

    if (availableCalls.isEmpty()) {
      return null;
    }

    if (availableCalls.size() > 1) {
      throw new IllegalStateException(
          "MatchPlayerDecisionView must expose at most one available truco call");
    }

    return availableCalls.getFirst();
  }

  private static MatchPlayerDecisionView.EnvidoOption toEnvidoOption(final EnvidoCall call,
      final int myScore, final int rivalScore, final int pointsToWin) {

    final var pointsIfPlayerWins =
        call == EnvidoCall.FALTA_ENVIDO ? pointsToWin - rivalScore : call.points();
    final var pointsIfRivalWins =
        call == EnvidoCall.FALTA_ENVIDO ? pointsToWin - myScore : call.points();
    return new MatchPlayerDecisionView.EnvidoOption(call, pointsIfPlayerWins, pointsIfRivalWins);
  }

  private static MatchPlayerDecisionView.PendingEnvidoOutcome toPendingEnvidoOutcome(
      final List<MatchPlayerDecisionView.EnvidoOption> currentChain) {

    if (currentChain.isEmpty()) {
      return null;
    }

    return new MatchPlayerDecisionView.PendingEnvidoOutcome(
        acceptedPoints(currentChain, MatchPlayerDecisionView.EnvidoOption::pointsIfPlayerWins),
        acceptedPoints(currentChain, MatchPlayerDecisionView.EnvidoOption::pointsIfRivalWins),
        rejectedPoints(currentChain));
  }

  private static int acceptedPoints(final List<MatchPlayerDecisionView.EnvidoOption> currentChain,
      final ToIntFunction<MatchPlayerDecisionView.EnvidoOption> pointsSelector) {

    return currentChain.stream().filter(option -> option.call() == EnvidoCall.FALTA_ENVIDO)
        .findFirst().map(pointsSelector::applyAsInt)
        .orElseGet(() -> currentChain.stream().mapToInt(pointsSelector).sum());
  }

  private static int rejectedPoints(final List<MatchPlayerDecisionView.EnvidoOption> currentChain) {

    if (currentChain.size() == 1) {
      return 1;
    }

    return currentChain.subList(0, currentChain.size() - 1).stream()
        .filter(option -> option.call() != EnvidoCall.FALTA_ENVIDO)
        .mapToInt(MatchPlayerDecisionView.EnvidoOption::pointsIfPlayerWins).sum();
  }

  private static boolean hasAction(final List<AvailableAction> availableActions,
      final ActionType type) {

    return availableActions.stream().anyMatch(action -> action.type() == type);
  }

  private static boolean foldWouldGiveGameToBot(final Match match, final int rivalScore,
      final List<AvailableAction> availableActions) {

    if (!hasAction(availableActions, ActionType.FOLD)) {
      return false;
    }

    return rivalScore + match.getCurrentRound().getTrucoPointsAtStake()
        > ScoringPolicy.pointsToWinGame();
  }

  @SuppressWarnings("unchecked")
  private static <E extends Enum<E>> E tryParse(final String value, final Class<E> enumClass) {

    if (value == null) {
      return null;
    }
    try {
      return Enum.valueOf(enumClass, value);
    } catch (final IllegalArgumentException ignored) {
      return null;
    }
  }

}
