package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CampaignBotUnlockedForCasualEvent extends CampaignDomainEvent {

  private final PlayerId rivalId;
  private final MatchId matchId;
  private final int gameNumber;

  public CampaignBotUnlockedForCasualEvent(final PlayerId playerId, final PlayerId rivalId,
      final MatchId matchId, final int gameNumber) {

    super("CAMPAIGN_BOT_UNLOCKED_FOR_CASUAL", playerId);
    this.rivalId = Objects.requireNonNull(rivalId);
    this.matchId = Objects.requireNonNull(matchId);
    this.gameNumber = gameNumber;
  }

  public PlayerId getRivalId() {

    return this.rivalId;
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

}
