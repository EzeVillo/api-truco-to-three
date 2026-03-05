package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class PlayerReadyEvent extends DomainEventBase {

  private final PlayerSeat seat;

  public PlayerReadyEvent(final PlayerSeat seat) {

    super("PLAYER_READY");
    this.seat = seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

}
