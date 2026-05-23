package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import java.time.Duration;

public class MatchTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final Duration idleTimeout;

  public MatchTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    super(timeoutScheduler, dispatcher);
    this.idleTimeout = idleTimeout;
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;
    final var matchId = event.getMatchId().value().toString();

    if (inner instanceof MatchFinishedEvent || inner instanceof MatchForfeitedEvent
        || inner instanceof MatchCancelledEvent || inner instanceof MatchAbandonedEvent) {
      cancelTimeout(EntityType.MATCH, matchId);
    } else {
      scheduleTimeoutFromNow(EntityType.MATCH, matchId, idleTimeout);
    }
  }

}
