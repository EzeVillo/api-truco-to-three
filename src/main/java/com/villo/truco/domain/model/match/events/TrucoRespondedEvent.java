package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.DomainEventBase;

public final class TrucoRespondedEvent extends DomainEventBase {

  private final PlayerSeat responderSeat;
  private final TrucoResponse response;
  private final TrucoCall call;

  public TrucoRespondedEvent(final PlayerSeat responderSeat, final TrucoResponse response,
      final TrucoCall call) {

    super("TRUCO_RESPONDED");
    this.responderSeat = responderSeat;
    this.response = response;
    this.call = call;
  }

  public PlayerSeat getResponderSeat() {

    return this.responderSeat;
  }

  public TrucoResponse getResponse() {

    return this.response;
  }

  public TrucoCall getCall() {

    return this.call;
  }

}
