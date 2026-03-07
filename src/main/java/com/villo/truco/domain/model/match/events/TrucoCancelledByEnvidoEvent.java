package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class TrucoCancelledByEnvidoEvent extends DomainEventBase {

  public TrucoCancelledByEnvidoEvent() {

    super("TRUCO_CANCELLED_BY_ENVIDO");
  }

}