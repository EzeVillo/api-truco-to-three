package com.villo.truco.campaign.application.services;

import com.villo.truco.application.ports.HiddenBotIdsProvider;
import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class CampaignHiddenBotIdsProvider implements HiddenBotIdsProvider {

  private final CampaignLadderProvider campaignLadderProvider;

  public CampaignHiddenBotIdsProvider(final CampaignLadderProvider campaignLadderProvider) {

    this.campaignLadderProvider = Objects.requireNonNull(campaignLadderProvider);
  }

  @Override
  public Set<PlayerId> hiddenBotIds() {

    return this.campaignLadderProvider.ladder().bots().stream().map(CampaignBot::playerId)
        .collect(Collectors.toUnmodifiableSet());
  }

}
