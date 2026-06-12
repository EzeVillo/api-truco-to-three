package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CampaignAllRivalsDefeatedEvent extends CampaignDomainEvent {

  private final MatchId matchId;
  private final int gameNumber;

  public CampaignAllRivalsDefeatedEvent(final PlayerId playerId, final MatchId matchId,
      final int gameNumber) {

    super("CAMPAIGN_ALL_RIVALS_DEFEATED", playerId);
    this.matchId = Objects.requireNonNull(matchId);
    this.gameNumber = gameNumber;
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

}
