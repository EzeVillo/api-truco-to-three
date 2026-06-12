package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.exceptions.InvalidCampaignVictoryException;

public final class CampaignScoringPolicy {

  private static final int POINTS_PER_GAME_OF_MARGIN = 100;

  private CampaignScoringPolicy() {

  }

  public static int pointsForVictory(final int gamesWonWinner, final int gamesWonLoser) {

    if (gamesWonWinner < 0 || gamesWonLoser < 0) {
      throw new InvalidCampaignVictoryException(gamesWonWinner, gamesWonLoser);
    }

    final var margin = Math.max(1, gamesWonWinner - gamesWonLoser);
    return POINTS_PER_GAME_OF_MARGIN * margin;
  }

}
