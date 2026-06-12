package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidCampaignPointsException extends DomainException {

  public InvalidCampaignPointsException(final int value) {

    super("campaign points must never be negative, but was: " + value);
  }

}
