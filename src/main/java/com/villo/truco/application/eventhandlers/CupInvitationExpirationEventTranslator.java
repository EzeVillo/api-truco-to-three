package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.ResourceBecameUnjoinable;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import java.util.Objects;

public final class CupInvitationExpirationEventTranslator implements
    CupDomainEventHandler<CupDomainEvent> {

  private final ApplicationEventPublisher publisher;

  public CupInvitationExpirationEventTranslator(final ApplicationEventPublisher publisher) {

    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    if (!(event instanceof CupStartedEvent || event instanceof CupCancelledEvent)) {
      return;
    }

    this.publisher.publish(
        new ResourceBecameUnjoinable("CUP", event.getCupId().value().toString()));
  }

}
