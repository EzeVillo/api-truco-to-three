package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import java.util.List;

public final class CompositeLeagueEventNotifier extends CompositeEventDispatcher implements
    LeagueEventNotifier {

  public CompositeLeagueEventNotifier(final List<LeagueDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<LeagueDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
