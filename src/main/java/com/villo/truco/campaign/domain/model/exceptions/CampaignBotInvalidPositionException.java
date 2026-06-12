package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignBotInvalidPositionException extends DomainException {

  public CampaignBotInvalidPositionException(final int position) {

    super("campaign bot position must be greater than zero, but was: " + position);
  }

}
