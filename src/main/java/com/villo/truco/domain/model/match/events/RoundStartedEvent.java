package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class RoundStartedEvent extends DomainEventBase {

  private final int roundNumber;
  private final PlayerSeat manoSeat;

  public RoundStartedEvent(final int roundNumber, final PlayerSeat manoSeat) {

    super("ROUND_STARTED");
    this.roundNumber = roundNumber;
    this.manoSeat = manoSeat;
  }

  public int getRoundNumber() {

    return this.roundNumber;
  }

  public PlayerSeat getManoSeat() {

    return this.manoSeat;
  }

}
