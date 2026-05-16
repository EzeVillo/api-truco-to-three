package com.villo.truco.profile.infrastructure.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.profile.application.services.ProfileAchievementTrackingService;
import com.villo.truco.profile.application.services.ProfilePlayerStatsTrackingService;
import java.util.Objects;

public final class ProfileMatchDomainEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final ProfileAchievementTrackingService profileAchievementTrackingService;
  private final ProfilePlayerStatsTrackingService profilePlayerStatsTrackingService;

  public ProfileMatchDomainEventHandler(
      final ProfileAchievementTrackingService profileAchievementTrackingService,
      final ProfilePlayerStatsTrackingService profilePlayerStatsTrackingService) {

    this.profileAchievementTrackingService = Objects.requireNonNull(
        profileAchievementTrackingService);
    this.profilePlayerStatsTrackingService = Objects.requireNonNull(
        profilePlayerStatsTrackingService);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    this.profileAchievementTrackingService.handle(event);
    this.profilePlayerStatsTrackingService.handle(event);
  }

}
