package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.model.chat.events.ChatDomainEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import java.util.List;

public final class CompositeChatEventNotifier extends CompositeEventDispatcher implements
    ChatEventNotifier {

  public CompositeChatEventNotifier(final List<? extends DomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<ChatDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
