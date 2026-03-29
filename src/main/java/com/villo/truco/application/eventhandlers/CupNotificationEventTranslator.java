package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.CupEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import java.util.Objects;

public final class CupNotificationEventTranslator implements CupDomainEventHandler<CupDomainEvent> {

  private final CupEventMapper mapper;
  private final ApplicationEventPublisher publisher;

  public CupNotificationEventTranslator(final CupEventMapper mapper,
      final ApplicationEventPublisher publisher) {

    this.mapper = Objects.requireNonNull(mapper);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    final var payload = this.mapper.map(event);
    this.publisher.publish(
        new CupEventNotification(event.getCupId(), event.getParticipants(), event.getEventType(),
            event.getTimestamp(), payload));
  }

}
