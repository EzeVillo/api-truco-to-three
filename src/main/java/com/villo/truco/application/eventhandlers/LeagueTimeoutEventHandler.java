package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import java.time.Duration;

public class LeagueTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private final Duration idleTimeout;

  public LeagueTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    super(timeoutScheduler, dispatcher);
    this.idleTimeout = idleTimeout;
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    final var leagueId = event.getLeagueId().value().toString();

    if (event instanceof LeagueCancelledEvent || event instanceof LeagueFinishedEvent) {
      cancelTimeout(EntityType.LEAGUE, leagueId);
    } else {
      scheduleTimeoutFromNow(EntityType.LEAGUE, leagueId, idleTimeout);
    }
  }

}
