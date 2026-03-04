package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.DomainEventBase;

public final class TrucoCalledEvent extends DomainEventBase {

  private final PlayerSeat callerSeat;
  private final TrucoCall call;

  public TrucoCalledEvent(final PlayerSeat callerSeat, final TrucoCall call) {

    super("TRUCO_CALLED");
    this.callerSeat = callerSeat;
    this.call = call;
  }

  public PlayerSeat getCallerSeat() {

    return this.callerSeat;
  }

  public TrucoCall getCall() {

    return this.call;
  }

}
