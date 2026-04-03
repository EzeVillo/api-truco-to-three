package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import java.util.Objects;

public final class SpectatorCleanupOnMatchEndEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final SpectatorshipLifecycleManager lifecycleManager;

  public SpectatorCleanupOnMatchEndEventHandler(
      final SpectatorshipLifecycleManager lifecycleManager) {

    this.lifecycleManager = Objects.requireNonNull(lifecycleManager);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (event instanceof MatchFinishedEvent || event instanceof MatchForfeitedEvent
        || event instanceof MatchAbandonedEvent || event instanceof MatchCancelledEvent) {
      this.lifecycleManager.clearMatchSpectators(event.getMatchId());
    }
  }

}
