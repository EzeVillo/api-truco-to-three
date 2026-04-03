package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.spectator.SpectatorshipStopReason;
import java.util.Objects;

public final class SpectatorAutoKickOnLeagueMatchActivatedEventHandler implements
    LeagueDomainEventHandler<LeagueMatchActivatedEvent> {

  private final SpectatorshipLifecycleManager lifecycleManager;

  public SpectatorAutoKickOnLeagueMatchActivatedEventHandler(
      final SpectatorshipLifecycleManager lifecycleManager) {

    this.lifecycleManager = Objects.requireNonNull(lifecycleManager);
  }

  @Override
  public Class<LeagueMatchActivatedEvent> eventType() {

    return LeagueMatchActivatedEvent.class;
  }

  @Override
  public void handle(final LeagueMatchActivatedEvent event) {

    event.getParticipants().forEach(playerId -> this.lifecycleManager.forceStop(playerId,
        SpectatorshipStopReason.PLAYER_BECAME_ACTIVE));
  }

}
