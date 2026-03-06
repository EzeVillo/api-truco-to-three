package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

final class ScoringPolicy {

  private ScoringPolicy() {

  }

  static GameEvaluation evaluate(final int scorePlayerOne, final int scorePlayerTwo,
      final int gamesWonPlayerOne, final int gamesWonPlayerTwo, final PlayerId playerOne,
      final PlayerId playerTwo, final MatchRules rules) {

    final boolean oneExceeded = scorePlayerOne > rules.pointsToWinGame();
    final boolean twoExceeded = scorePlayerTwo > rules.pointsToWinGame();
    final boolean oneWon = scorePlayerOne == rules.pointsToWinGame();
    final boolean twoWon = scorePlayerTwo == rules.pointsToWinGame();

    final PlayerId gameWinner;
    if (oneExceeded) {
      gameWinner = playerTwo;
    } else if (twoExceeded) {
      gameWinner = playerOne;
    } else if (oneWon) {
      gameWinner = playerOne;
    } else if (twoWon) {
      gameWinner = playerTwo;
    } else {
      return GameEvaluation.NO_WINNER;
    }

    final int projectedGamesOne =
        gameWinner.equals(playerOne) ? gamesWonPlayerOne + 1 : gamesWonPlayerOne;
    final int projectedGamesTwo =
        gameWinner.equals(playerTwo) ? gamesWonPlayerTwo + 1 : gamesWonPlayerTwo;
    final boolean matchFinished =
        projectedGamesOne >= rules.gamesToWin() || projectedGamesTwo >= rules.gamesToWin();

    return new GameEvaluation(gameWinner, matchFinished);
  }

  record GameEvaluation(PlayerId gameWinner, boolean matchFinished) {

    static final GameEvaluation NO_WINNER = new GameEvaluation(null, false);

    boolean isGameOver() {

      return this.gameWinner != null;
    }

  }

}
