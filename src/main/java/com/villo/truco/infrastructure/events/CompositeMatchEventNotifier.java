package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CompositeMatchEventNotifier implements MatchEventNotifier {

  private final List<MatchDomainEventHandler<?>> handlers;

  public CompositeMatchEventNotifier(final List<MatchDomainEventHandler<?>> handlers) {

    this.handlers = List.copyOf(Objects.requireNonNull(handlers));
  }

  @Override
  public void publishDomainEvents(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final List<DomainEventBase> events) {

    final var context = new MatchEventContext(matchId, playerOne, playerTwo);

    for (final var event : events) {
      for (final var handler : this.handlers) {
        if (handler.eventType().isAssignableFrom(event.getClass()) || event.getClass()
            .isAssignableFrom(handler.eventType())) {
          this.invokeHandler(handler, event, context);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends DomainEventBase> void invokeHandler(final MatchDomainEventHandler<E> handler,
      final DomainEventBase event, final MatchEventContext context) {

    handler.handle((E) event, context);
  }

}
