package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.ResourceBecameUnjoinable;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import java.util.Objects;

public final class MatchInvitationExpirationEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final ApplicationEventPublisher publisher;

  public MatchInvitationExpirationEventTranslator(final ApplicationEventPublisher publisher) {

    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (!(event instanceof PlayerJoinedEvent || event instanceof MatchCancelledEvent)) {
      return;
    }

    this.publisher.publish(
        new ResourceBecameUnjoinable("MATCH", event.getMatchId().value().toString()));
  }

}
