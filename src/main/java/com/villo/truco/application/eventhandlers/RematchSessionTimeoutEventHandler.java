package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;

public class RematchSessionTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    RematchSessionDomainEventHandler<RematchSessionDomainEvent> {

  public RematchSessionTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher) {

    super(timeoutScheduler, dispatcher);
  }

  @Override
  public Class<RematchSessionDomainEvent> eventType() {

    return RematchSessionDomainEvent.class;
  }

  @Override
  public void handle(final RematchSessionDomainEvent event) {

    final var sessionId = event.getRematchSessionId().value().toString();

    if (event instanceof RematchSessionConfirmedEvent
        || event instanceof RematchSessionClosedByLeaveEvent
        || event instanceof RematchSessionExpiredEvent) {
      cancelTimeout(EntityType.REMATCH_SESSION, sessionId);
    } else if (event instanceof RematchSessionOpenedEvent opened) {
      scheduleTimeout(EntityType.REMATCH_SESSION, sessionId, opened.getExpiresAt());
    }
  }

}
