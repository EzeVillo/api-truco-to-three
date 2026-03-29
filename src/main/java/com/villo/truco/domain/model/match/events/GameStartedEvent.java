package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class GameStartedEvent extends MatchDomainEvent {

  private final int gameNumber;

  public GameStartedEvent(final MatchId matchId, final PlayerId playerOne, final PlayerId playerTwo,
      final int gameNumber) {

    super("GAME_STARTED", matchId, playerOne, playerTwo);
    this.gameNumber = gameNumber;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

}
