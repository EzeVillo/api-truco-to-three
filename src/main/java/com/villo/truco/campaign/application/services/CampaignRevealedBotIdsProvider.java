package com.villo.truco.campaign.application.services;

import com.villo.truco.application.ports.RevealedBotIdsProvider;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;
import java.util.Set;

public final class CampaignRevealedBotIdsProvider implements RevealedBotIdsProvider {

  private final CampaignProgressRepository campaignProgressRepository;

  public CampaignRevealedBotIdsProvider(
      final CampaignProgressRepository campaignProgressRepository) {

    this.campaignProgressRepository = Objects.requireNonNull(campaignProgressRepository);
  }

  @Override
  public Set<PlayerId> revealedBotIds(final PlayerId playerId) {

    return this.campaignProgressRepository.findByPlayerId(playerId)
        .map(CampaignProgress::getUnlockedCasualBots).orElseGet(Set::of);
  }

}
