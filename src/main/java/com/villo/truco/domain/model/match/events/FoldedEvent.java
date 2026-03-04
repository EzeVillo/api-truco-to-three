package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class FoldedEvent extends DomainEventBase {

  private final PlayerSeat seat;

  public FoldedEvent(final PlayerSeat seat) {

    super("FOLDED");
    this.seat = seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

}
