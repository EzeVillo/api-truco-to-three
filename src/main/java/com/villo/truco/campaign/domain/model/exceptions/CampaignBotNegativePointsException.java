package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignBotNegativePointsException extends DomainException {

  public CampaignBotNegativePointsException(final int points) {

    super("campaign bot points must never be negative, but was: " + points);
  }

}
