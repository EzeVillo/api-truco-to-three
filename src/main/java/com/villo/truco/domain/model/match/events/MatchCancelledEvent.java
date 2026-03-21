package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class MatchCancelledEvent extends DomainEventBase {

  public MatchCancelledEvent() {

    super("MATCH_CANCELLED");
  }

}
