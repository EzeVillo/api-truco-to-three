package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.util.List;

public final class CompositeSocialEventNotifier extends CompositeEventDispatcher implements
    SocialEventNotifier {

  public CompositeSocialEventNotifier(final List<? extends DomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<? extends SocialDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
