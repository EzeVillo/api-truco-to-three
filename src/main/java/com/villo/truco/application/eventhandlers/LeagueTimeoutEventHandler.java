package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.application.timeout.LeagueTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import java.time.Duration;
import java.util.Objects;

public class LeagueTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private final LeagueTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;

  public LeagueTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final LeagueTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout) {

    super(timeoutScheduler, dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    final var leagueId = event.getLeagueId().value().toString();
    if (this.phasePolicy.phaseOf(event) == TimeoutPhase.LOBBY) {
      scheduleTimeoutFromNow(EntityType.LEAGUE, leagueId, this.lobbyTimeout);
    } else {
      cancelTimeout(EntityType.LEAGUE, leagueId);
    }
  }

}
