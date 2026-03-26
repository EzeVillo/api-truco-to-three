package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class CompositeMatchEventNotifier
    extends CompositeEventDispatcher<MatchEventContext>
    implements MatchEventNotifier {

  public CompositeMatchEventNotifier(final List<MatchDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final List<DomainEventBase> events) {

    this.dispatchEvents(new MatchEventContext(matchId, playerOne, playerTwo), events);
  }

}
