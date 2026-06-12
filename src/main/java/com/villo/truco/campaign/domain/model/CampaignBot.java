package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.exceptions.CampaignBotDisplayNameBlankException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignBotInvalidPositionException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignBotNegativePointsException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record CampaignBot(PlayerId playerId, String displayName, int position, int points) {

  public CampaignBot {

    Objects.requireNonNull(playerId, "playerId cannot be null");
    Objects.requireNonNull(displayName, "displayName cannot be null");

    if (displayName.isBlank()) {
      throw new CampaignBotDisplayNameBlankException();
    }
    if (position <= 0) {
      throw new CampaignBotInvalidPositionException(position);
    }
    if (points < 0) {
      throw new CampaignBotNegativePointsException(points);
    }
  }

}
