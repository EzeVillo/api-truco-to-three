package com.villo.truco.campaign.infrastructure.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.campaign.application.services.CampaignChallengeResolutionService;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import java.util.Objects;

public final class CampaignMatchDomainEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final CampaignChallengeResolutionService campaignChallengeResolutionService;

  public CampaignMatchDomainEventHandler(
      final CampaignChallengeResolutionService campaignChallengeResolutionService) {

    this.campaignChallengeResolutionService = Objects.requireNonNull(
        campaignChallengeResolutionService);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    this.campaignChallengeResolutionService.handle(event);
  }

}
