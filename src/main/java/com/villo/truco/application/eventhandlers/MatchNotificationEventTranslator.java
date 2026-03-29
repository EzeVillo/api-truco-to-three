package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import java.util.Objects;

public final class MatchNotificationEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final MatchEventMapper mapper;
  private final MatchRecipientResolver recipientResolver;
  private final ApplicationEventPublisher publisher;

  public MatchNotificationEventTranslator(final MatchEventMapper mapper,
      final MatchRecipientResolver recipientResolver, final ApplicationEventPublisher publisher) {

    this.mapper = Objects.requireNonNull(mapper);
    this.recipientResolver = Objects.requireNonNull(recipientResolver);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;
    final var payload = this.mapper.map(inner);
    final var recipients = this.recipientResolver.resolve(event, inner);
    this.publisher.publish(
        new MatchEventNotification(event.getMatchId(), recipients, inner.getEventType(),
            inner.getTimestamp(), payload));
  }

}
