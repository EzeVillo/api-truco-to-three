package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class RoundEndedEvent extends DomainEventBase {

  private final PlayerSeat winnerSeat;

  public RoundEndedEvent(final PlayerSeat winnerSeat) {

    super("ROUND_ENDED");
    this.winnerSeat = winnerSeat;
  }

  public PlayerSeat getWinnerSeat() {

    return this.winnerSeat;
  }

}
