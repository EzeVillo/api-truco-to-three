package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

final class MatchLifecyclePolicy {

  private MatchLifecyclePolicy() {

  }

  static GameStartState startNextGame(final int currentGameNumber, final PlayerId playerOne,
      final PlayerId playerTwo) {

    Objects.requireNonNull(playerOne, "Player one cannot be null");
    Objects.requireNonNull(playerTwo, "Player two cannot be null");

    final var nextGameNumber = currentGameNumber + 1;
    final var firstManoOfGame = nextGameNumber % 2 == 1 ? playerOne : playerTwo;

    return new GameStartState(nextGameNumber, 0, 0, 0, firstManoOfGame);
  }

  static PlayerId resolveRoundMano(final int nextRoundNumber, final PlayerId firstManoOfGame,
      final PlayerId playerOne, final PlayerId playerTwo) {

    Objects.requireNonNull(firstManoOfGame, "First mano of game cannot be null");
    Objects.requireNonNull(playerOne, "Player one cannot be null");
    Objects.requireNonNull(playerTwo, "Player two cannot be null");

    if (nextRoundNumber % 2 == 1) {
      return firstManoOfGame;
    }

    return firstManoOfGame.equals(playerOne) ? playerTwo : playerOne;
  }

  record GameStartState(int gameNumber, int scorePlayerOne, int scorePlayerTwo, int roundNumber,
                        PlayerId firstManoOfGame) {

  }

}
