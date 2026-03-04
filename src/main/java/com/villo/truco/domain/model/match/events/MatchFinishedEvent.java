package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class MatchFinishedEvent extends DomainEventBase {

  private final PlayerSeat winnerSeat;
  private final int gamesWonPlayerOne;
  private final int gamesWonPlayerTwo;

  public MatchFinishedEvent(final PlayerSeat winnerSeat, final int gamesWonPlayerOne,
      final int gamesWonPlayerTwo) {

    super("MATCH_FINISHED");
    this.winnerSeat = winnerSeat;
    this.gamesWonPlayerOne = gamesWonPlayerOne;
    this.gamesWonPlayerTwo = gamesWonPlayerTwo;
  }

  public PlayerSeat getWinnerSeat() {

    return this.winnerSeat;
  }

  public int getGamesWonPlayerOne() {

    return this.gamesWonPlayerOne;
  }

  public int getGamesWonPlayerTwo() {

    return this.gamesWonPlayerTwo;
  }

}
