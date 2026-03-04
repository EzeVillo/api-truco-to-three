package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class TurnChangedEvent extends DomainEventBase {

  private final PlayerSeat seat;

  public TurnChangedEvent(final PlayerSeat seat) {

    super("TURN_CHANGED");
    this.seat = seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

}
