package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.application.timeout.CupTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import java.time.Duration;
import java.util.Objects;

public class CupTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    CupDomainEventHandler<CupDomainEvent> {

  private final CupTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;

  public CupTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final CupTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout) {

    super(timeoutScheduler, dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    final var cupId = event.getCupId().value().toString();
    if (this.phasePolicy.phaseOf(event) == TimeoutPhase.LOBBY) {
      scheduleTimeoutFromNow(EntityType.CUP, cupId, this.lobbyTimeout);
    } else {
      cancelTimeout(EntityType.CUP, cupId);
    }
  }

}
