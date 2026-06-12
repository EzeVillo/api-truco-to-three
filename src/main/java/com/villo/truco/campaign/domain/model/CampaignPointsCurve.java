package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderInvalidCurveInputException;

public final class CampaignPointsCurve {

  private static final double PROGRESSION_EXPONENT = 2.0;
  private static final int ROUNDING_STEP = 5;

  private CampaignPointsCurve() {

  }

  public static int pointsForPosition(final int position, final int totalBots,
      final int topPoints) {

    if (position < 1 || position > totalBots || topPoints <= 0) {
      throw new CampaignLadderInvalidCurveInputException(position, totalBots, topPoints);
    }

    final var normalizedRank = (double) (totalBots + 1 - position) / totalBots;
    final var rawPoints = topPoints * Math.pow(normalizedRank, PROGRESSION_EXPONENT);
    return (int) Math.round(rawPoints / ROUNDING_STEP) * ROUNDING_STEP;
  }

}
