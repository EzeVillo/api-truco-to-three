package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import java.util.List;

public final class RematchSessionDomainEventDispatcher extends CompositeEventDispatcher implements
    RematchSessionEventNotifier {

  public RematchSessionDomainEventDispatcher(
      final List<RematchSessionDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<RematchSessionDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
