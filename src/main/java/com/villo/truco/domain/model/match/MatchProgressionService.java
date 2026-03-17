package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

final class MatchProgressionService {

  private MatchProgressionService() {

  }

  static ProgressionResult applyPoints(final int scorePlayerOne, final int scorePlayerTwo,
      final int gamesWonPlayerOne, final int gamesWonPlayerTwo, final PlayerId playerOne,
      final PlayerId playerTwo, final MatchRules rules, final PlayerId pointsWinner,
      final int points) {

    Objects.requireNonNull(playerOne, "Player one cannot be null");
    Objects.requireNonNull(playerTwo, "Player two cannot be null");
    Objects.requireNonNull(rules, "Rules cannot be null");
    Objects.requireNonNull(pointsWinner, "Points winner cannot be null");

    final int nextScorePlayerOne;
    final int nextScorePlayerTwo;

    if (pointsWinner.equals(playerOne)) {
      nextScorePlayerOne = scorePlayerOne + points;
      nextScorePlayerTwo = scorePlayerTwo;
    } else {
      nextScorePlayerOne = scorePlayerOne;
      nextScorePlayerTwo = scorePlayerTwo + points;
    }

    final var evaluation = ScoringPolicy.evaluate(nextScorePlayerOne, nextScorePlayerTwo,
        gamesWonPlayerOne, gamesWonPlayerTwo, playerOne, playerTwo, rules);

    if (!evaluation.isGameOver()) {
      return new ProgressionResult(nextScorePlayerOne, nextScorePlayerTwo, gamesWonPlayerOne,
          gamesWonPlayerTwo, null, false, false);
    }

    final var gameWinner = evaluation.gameWinner();

    final int nextGamesWonPlayerOne;
    final int nextGamesWonPlayerTwo;

    if (gameWinner.equals(playerOne)) {
      nextGamesWonPlayerOne = gamesWonPlayerOne + 1;
      nextGamesWonPlayerTwo = gamesWonPlayerTwo;
    } else {
      nextGamesWonPlayerOne = gamesWonPlayerOne;
      nextGamesWonPlayerTwo = gamesWonPlayerTwo + 1;
    }

    return new ProgressionResult(nextScorePlayerOne, nextScorePlayerTwo, nextGamesWonPlayerOne,
        nextGamesWonPlayerTwo, gameWinner, true, evaluation.matchFinished());
  }

  record ProgressionResult(int scorePlayerOne, int scorePlayerTwo, int gamesWonPlayerOne,
                           int gamesWonPlayerTwo, PlayerId gameWinner, boolean gameOver,
                           boolean matchFinished) {

  }

}
