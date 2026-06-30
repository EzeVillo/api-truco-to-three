package com.villo.truco.history.infrastructure.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.history.application.services.MatchHistoryTrackingService;
import java.util.Objects;

public final class HistoryMatchDomainEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final MatchHistoryTrackingService matchHistoryTrackingService;

  public HistoryMatchDomainEventHandler(
      final MatchHistoryTrackingService matchHistoryTrackingService) {

    this.matchHistoryTrackingService = Objects.requireNonNull(matchHistoryTrackingService);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    this.matchHistoryTrackingService.handle(event);
  }

}
