package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.ChatEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.model.chat.events.ChatDomainEvent;
import com.villo.truco.domain.model.chat.events.ChatEventEnvelope;
import java.util.Objects;

public final class ChatNotificationEventTranslator implements DomainEventHandler<ChatDomainEvent> {

  private final ChatEventMapper mapper;
  private final ApplicationEventPublisher publisher;

  public ChatNotificationEventTranslator(final ChatEventMapper mapper,
      final ApplicationEventPublisher publisher) {

    this.mapper = Objects.requireNonNull(mapper);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<ChatDomainEvent> eventType() {

    return ChatDomainEvent.class;
  }

  @Override
  public void handle(final ChatDomainEvent event) {

    final var inner = event instanceof ChatEventEnvelope env ? env.getInner() : event;
    this.publisher.publish(
        new ChatEventNotification(event.getChatId(), event.getParticipants(), inner.getEventType(),
            inner.getTimestamp(), this.mapper.map(inner)));
  }

}
