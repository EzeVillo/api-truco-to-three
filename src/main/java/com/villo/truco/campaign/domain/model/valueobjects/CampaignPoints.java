package com.villo.truco.campaign.domain.model.valueobjects;

import com.villo.truco.campaign.domain.model.exceptions.InvalidCampaignPointsException;

public record CampaignPoints(int value) {

  public static final CampaignPoints ZERO = new CampaignPoints(0);

  public CampaignPoints {

    if (value < 0) {
      throw new InvalidCampaignPointsException(value);
    }
  }

  public CampaignPoints plus(final int pointsToAdd) {

    if (pointsToAdd < 0) {
      throw new InvalidCampaignPointsException(pointsToAdd);
    }
    return new CampaignPoints(this.value + pointsToAdd);
  }

}
