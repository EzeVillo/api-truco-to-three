package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.cup.events.CupMatchActivatedEvent;
import com.villo.truco.domain.model.spectator.SpectatorshipStopReason;
import java.util.Objects;

public final class SpectatorAutoKickOnCupMatchActivatedEventHandler implements
    CupDomainEventHandler<CupMatchActivatedEvent> {

  private final SpectatorshipLifecycleManager lifecycleManager;

  public SpectatorAutoKickOnCupMatchActivatedEventHandler(
      final SpectatorshipLifecycleManager lifecycleManager) {

    this.lifecycleManager = Objects.requireNonNull(lifecycleManager);
  }

  @Override
  public Class<CupMatchActivatedEvent> eventType() {

    return CupMatchActivatedEvent.class;
  }

  @Override
  public void handle(final CupMatchActivatedEvent event) {

    event.getParticipants().forEach(playerId -> this.lifecycleManager.forceStop(playerId,
        SpectatorshipStopReason.PLAYER_BECAME_ACTIVE));
  }

}
