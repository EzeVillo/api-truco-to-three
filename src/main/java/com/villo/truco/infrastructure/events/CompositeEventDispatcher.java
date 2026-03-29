package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;
import java.util.Objects;

public abstract class CompositeEventDispatcher {

  private final List<DomainEventHandler<?>> handlers;

  protected CompositeEventDispatcher(final List<? extends DomainEventHandler<?>> handlers) {

    this.handlers = List.copyOf(Objects.requireNonNull(handlers));
  }

  protected final void dispatchEvents(final List<? extends DomainEventBase> events) {

    for (final var event : events) {
      for (final var handler : this.handlers) {
        if (handler.eventType().isAssignableFrom(event.getClass()) || event.getClass()
            .isAssignableFrom(handler.eventType())) {
          this.invokeHandler(handler, event);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends DomainEventBase> void invokeHandler(final DomainEventHandler<E> handler,
      final DomainEventBase event) {

    handler.handle((E) event);
  }

}
