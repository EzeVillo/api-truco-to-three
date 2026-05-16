package com.villo.truco.auth.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.auth.domain.model.user.events.AuthDomainEvent;
import com.villo.truco.auth.domain.ports.AuthEventNotifier;
import com.villo.truco.infrastructure.events.CompositeEventDispatcher;
import java.util.List;

public final class CompositeAuthEventNotifier extends CompositeEventDispatcher implements
    AuthEventNotifier {

  public CompositeAuthEventNotifier(final List<? extends DomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<AuthDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
