package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.social.application.events.SocialEventNotification;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import java.util.Objects;

public final class SocialNotificationEventTranslator implements
    DomainEventHandler<SocialDomainEvent> {

  private final SocialEventMapper socialEventMapper;
  private final ApplicationEventPublisher applicationEventPublisher;

  public SocialNotificationEventTranslator(final SocialEventMapper socialEventMapper,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.socialEventMapper = Objects.requireNonNull(socialEventMapper);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  @Override
  public Class<SocialDomainEvent> eventType() {

    return SocialDomainEvent.class;
  }

  @Override
  public void handle(final SocialDomainEvent event) {

    this.applicationEventPublisher.publish(
        new SocialEventNotification(event.getRecipients(), event.getEventType(),
            event.getTimestamp(), this.socialEventMapper.map(event)));
  }

}
