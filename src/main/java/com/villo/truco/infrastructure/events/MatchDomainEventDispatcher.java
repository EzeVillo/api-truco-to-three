package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.ports.MatchEventNotifier;
import java.util.List;

public final class MatchDomainEventDispatcher extends CompositeEventDispatcher implements
    MatchEventNotifier {

  public MatchDomainEventDispatcher(final List<MatchDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<MatchDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
