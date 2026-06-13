package com.villo.truco.application.timeout;

import com.villo.truco.domain.model.cup.events.CupCreatedEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;

public final class CupTimeoutPhasePolicy {

  public TimeoutPhase phaseOf(final CupStatus status) {

    return switch (status) {
      case WAITING_FOR_PLAYERS, WAITING_FOR_START -> TimeoutPhase.LOBBY;
      case IN_PROGRESS, FINISHED, CANCELLED -> TimeoutPhase.NONE;
    };
  }

  public TimeoutPhase phaseOf(final CupDomainEvent event) {

    if (event instanceof CupCreatedEvent || event instanceof CupPlayerJoinedEvent
        || event instanceof CupPlayerLeftEvent) {
      return TimeoutPhase.LOBBY;
    }
    return TimeoutPhase.NONE;
  }

}
