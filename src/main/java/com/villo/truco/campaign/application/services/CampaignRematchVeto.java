package com.villo.truco.campaign.application.services;

import com.villo.truco.application.ports.RematchVeto;
import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public final class CampaignRematchVeto implements RematchVeto {

  private final CampaignMatchRegistry campaignMatchRegistry;

  public CampaignRematchVeto(final CampaignMatchRegistry campaignMatchRegistry) {

    this.campaignMatchRegistry = Objects.requireNonNull(campaignMatchRegistry);
  }

  @Override
  public boolean vetoesRematch(final MatchId matchId) {

    return this.campaignMatchRegistry.isCampaignMatch(matchId);
  }

}
