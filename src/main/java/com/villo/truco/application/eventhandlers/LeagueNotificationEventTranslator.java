package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.LeagueEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import java.util.Objects;

public final class LeagueNotificationEventTranslator implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private final LeagueEventMapper mapper;
  private final ApplicationEventPublisher publisher;

  public LeagueNotificationEventTranslator(final LeagueEventMapper mapper,
      final ApplicationEventPublisher publisher) {

    this.mapper = Objects.requireNonNull(mapper);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    final var payload = this.mapper.map(event);
    this.publisher.publish(new LeagueEventNotification(event.getLeagueId(), event.getParticipants(),
        event.getEventType(), event.getTimestamp(), payload));
  }

}
