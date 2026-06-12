package com.villo.truco.campaign.application.usecases.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record StartCampaignChallengeCommand(PlayerId playerId, PlayerId botId) {

  public StartCampaignChallengeCommand(final String playerId, final String botId) {

    this(PlayerId.of(playerId), botId == null ? null : PlayerId.of(botId));
  }

}
