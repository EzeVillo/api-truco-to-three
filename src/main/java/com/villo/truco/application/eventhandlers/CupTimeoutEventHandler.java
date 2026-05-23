package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import java.time.Duration;

public class CupTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    CupDomainEventHandler<CupDomainEvent> {

  private final Duration idleTimeout;

  public CupTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    super(timeoutScheduler, dispatcher);
    this.idleTimeout = idleTimeout;
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    final var cupId = event.getCupId().value().toString();

    if (event instanceof CupCancelledEvent || event instanceof CupFinishedEvent) {
      cancelTimeout(EntityType.CUP, cupId);
    } else {
      scheduleTimeoutFromNow(EntityType.CUP, cupId, idleTimeout);
    }
  }

}
