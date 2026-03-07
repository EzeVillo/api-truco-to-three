package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class GameScoreChangedEvent extends DomainEventBase {

  private final int gamesWonPlayerOne;
  private final int gamesWonPlayerTwo;

  public GameScoreChangedEvent(final int gamesWonPlayerOne, final int gamesWonPlayerTwo) {

    super("GAME_SCORE_CHANGED");
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