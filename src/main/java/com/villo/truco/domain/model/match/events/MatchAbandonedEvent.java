package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class MatchAbandonedEvent extends MatchDomainEvent {

  private final PlayerSeat winnerSeat;
  private final PlayerSeat abandonerSeat;
  private final int gamesWonPlayerOne;
  private final int gamesWonPlayerTwo;

  public MatchAbandonedEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final PlayerSeat winnerSeat, final PlayerSeat abandonerSeat,
      final int gamesWonPlayerOne, final int gamesWonPlayerTwo) {

    super("MATCH_ABANDONED", matchId, playerOne, playerTwo);
    this.winnerSeat = winnerSeat;
    this.abandonerSeat = abandonerSeat;
    this.gamesWonPlayerOne = gamesWonPlayerOne;
    this.gamesWonPlayerTwo = gamesWonPlayerTwo;
  }

  public PlayerSeat getWinnerSeat() {

    return this.winnerSeat;
  }

  public PlayerSeat getAbandonerSeat() {

    return this.abandonerSeat;
  }

  public int getGamesWonPlayerOne() {

    return this.gamesWonPlayerOne;
  }

  public int getGamesWonPlayerTwo() {

    return this.gamesWonPlayerTwo;
  }

}
