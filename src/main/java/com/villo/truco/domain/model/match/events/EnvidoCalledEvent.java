package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;

public final class EnvidoCalledEvent extends DomainEventBase {

  private final PlayerSeat callerSeat;
  private final EnvidoCall call;

  public EnvidoCalledEvent(final PlayerSeat callerSeat, final EnvidoCall call) {

    super("ENVIDO_CALLED");
    this.callerSeat = callerSeat;
    this.call = call;
  }

  public PlayerSeat getCallerSeat() {

    return this.callerSeat;
  }

  public EnvidoCall getCall() {

    return this.call;
  }

}
