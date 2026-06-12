package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CampaignAllRivalsDefeatedEvent extends CampaignDomainEvent {

  private final MatchId matchId;

  public CampaignAllRivalsDefeatedEvent(final PlayerId playerId, final MatchId matchId) {

    super("CAMPAIGN_ALL_RIVALS_DEFEATED", playerId);
    this.matchId = Objects.requireNonNull(matchId);
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

}
