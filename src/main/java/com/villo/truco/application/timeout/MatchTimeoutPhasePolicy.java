package com.villo.truco.application.timeout;

import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchCreatedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.MatchPlayerLeftEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.events.SeatTargetedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.shared.DomainEventBase;

public final class MatchTimeoutPhasePolicy {

  public TimeoutPhase phaseOf(final MatchStatus status) {

    return switch (status) {
      case WAITING_FOR_PLAYERS, READY -> TimeoutPhase.LOBBY;
      case IN_PROGRESS -> TimeoutPhase.PLAY;
      case FINISHED, CANCELLED -> TimeoutPhase.NONE;
    };
  }

  public TimeoutPhase phaseOf(final DomainEventBase event) {

    if (event instanceof MatchFinishedEvent || event instanceof MatchForfeitedEvent
        || event instanceof MatchCancelledEvent || event instanceof MatchAbandonedEvent) {
      return TimeoutPhase.NONE;
    }
    if (event instanceof MatchCreatedEvent || event instanceof PlayerJoinedEvent
        || event instanceof PlayerReadyEvent || event instanceof SeatTargetedEvent
        || event instanceof MatchPlayerLeftEvent) {
      return TimeoutPhase.LOBBY;
    }
    return TimeoutPhase.PLAY;
  }

}
