package com.villo.truco.profile.infrastructure.eventhandlers;

import com.villo.truco.campaign.application.ports.out.CampaignDomainEventHandler;
import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.profile.application.services.ProfileCampaignAchievementService;
import java.util.Objects;

public final class ProfileCampaignDomainEventHandler implements
    CampaignDomainEventHandler<CampaignDomainEvent> {

  private final ProfileCampaignAchievementService profileCampaignAchievementService;

  public ProfileCampaignDomainEventHandler(
      final ProfileCampaignAchievementService profileCampaignAchievementService) {

    this.profileCampaignAchievementService = Objects.requireNonNull(
        profileCampaignAchievementService);
  }

  @Override
  public Class<CampaignDomainEvent> eventType() {

    return CampaignDomainEvent.class;
  }

  @Override
  public void handle(final CampaignDomainEvent event) {

    this.profileCampaignAchievementService.handle(event);
  }

}
