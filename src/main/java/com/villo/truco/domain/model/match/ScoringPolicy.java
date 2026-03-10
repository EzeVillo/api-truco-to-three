package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

final class ScoringPolicy {

  private static final int POINTS_TO_WIN_GAME = 3;

  private ScoringPolicy() {

  }

  static GameEvaluation evaluate(final int scorePlayerOne, final int scorePlayerTwo,
      final int gamesWonPlayerOne, final int gamesWonPlayerTwo, final PlayerId playerOne,
      final PlayerId playerTwo, final MatchRules rules) {

    final boolean oneExceeded = scorePlayerOne > POINTS_TO_WIN_GAME;
    final boolean twoExceeded = scorePlayerTwo > POINTS_TO_WIN_GAME;
    final boolean oneWon = scorePlayerOne == POINTS_TO_WIN_GAME;
    final boolean twoWon = scorePlayerTwo == POINTS_TO_WIN_GAME;

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

    public boolean isGameOver() {

      return this.gameWinner != null;
    }

  }

}
