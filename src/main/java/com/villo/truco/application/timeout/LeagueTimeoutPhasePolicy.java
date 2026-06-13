package com.villo.truco.application.timeout;

import com.villo.truco.domain.model.league.events.LeagueCreatedEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;

public final class LeagueTimeoutPhasePolicy {

  public TimeoutPhase phaseOf(final LeagueStatus status) {

    return switch (status) {
      case WAITING_FOR_PLAYERS, WAITING_FOR_START -> TimeoutPhase.LOBBY;
      case IN_PROGRESS, FINISHED, CANCELLED -> TimeoutPhase.NONE;
    };
  }

  public TimeoutPhase phaseOf(final LeagueDomainEvent event) {

    if (event instanceof LeagueCreatedEvent || event instanceof LeaguePlayerJoinedEvent
        || event instanceof LeaguePlayerLeftEvent) {
      return TimeoutPhase.LOBBY;
    }
    return TimeoutPhase.NONE;
  }

}
