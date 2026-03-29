package com.villo.truco.infrastructure.events;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import java.util.List;
import java.util.Objects;

public final class InProcessApplicationEventPublisher implements ApplicationEventPublisher {

  private final List<ApplicationEventHandler<?>> handlers;

  public InProcessApplicationEventPublisher(final List<ApplicationEventHandler<?>> handlers) {

    this.handlers = List.copyOf(Objects.requireNonNull(handlers));
  }

  @Override
  public void publish(final ApplicationEvent event) {

    for (final var handler : this.handlers) {
      if (handler.eventType().isAssignableFrom(event.getClass())) {
        this.invokeHandler(handler, event);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends ApplicationEvent> void invokeHandler(final ApplicationEventHandler<E> handler,
      final ApplicationEvent event) {

    handler.handle((E) event);
  }

}
