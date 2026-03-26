package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;
import java.util.Objects;

public abstract class CompositeEventDispatcher<C> {

    private final List<DomainEventHandler<?, C>> handlers;

    protected CompositeEventDispatcher(
        final List<? extends DomainEventHandler<?, C>> handlers) {

        this.handlers = List.copyOf(Objects.requireNonNull(handlers));
    }

    protected final void dispatchEvents(final C context, final List<DomainEventBase> events) {

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
    private <E extends DomainEventBase> void invokeHandler(final DomainEventHandler<E, C> handler,
        final DomainEventBase event, final C context) {

        handler.handle((E) event, context);
    }

}
