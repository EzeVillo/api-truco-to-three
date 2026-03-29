package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class GameScoreChangedEvent extends MatchDomainEvent {

  private final int gamesWonPlayerOne;
  private final int gamesWonPlayerTwo;

  public GameScoreChangedEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final int gamesWonPlayerOne, final int gamesWonPlayerTwo) {

    super("GAME_SCORE_CHANGED", matchId, playerOne, playerTwo);
    this.gamesWonPlayerOne = gamesWonPlayerOne;
    this.gamesWonPlayerTwo = gamesWonPlayerTwo;
  }

  public int getGamesWonPlayerOne() {

    return this.gamesWonPlayerOne;
  }

  public int getGamesWonPlayerTwo() {

    return this.gamesWonPlayerTwo;
  }

}