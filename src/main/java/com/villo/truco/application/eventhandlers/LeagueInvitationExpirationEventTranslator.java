package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.ResourceBecameUnjoinable;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import java.util.Objects;

public final class LeagueInvitationExpirationEventTranslator implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private final ApplicationEventPublisher publisher;

  public LeagueInvitationExpirationEventTranslator(final ApplicationEventPublisher publisher) {

    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    if (!(event instanceof LeagueStartedEvent || event instanceof LeagueCancelledEvent)) {
      return;
    }

    this.publisher.publish(
        new ResourceBecameUnjoinable("LEAGUE", event.getLeagueId().value().toString()));
  }

}
