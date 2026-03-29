package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.ports.CupEventNotifier;
import java.util.List;

public final class CompositeCupEventNotifier extends CompositeEventDispatcher implements
    CupEventNotifier {

  public CompositeCupEventNotifier(final List<CupDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<CupDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
