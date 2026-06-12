package com.villo.truco.campaign.application.usecases.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record GetCampaignQuery(PlayerId playerId) {

  public GetCampaignQuery(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
