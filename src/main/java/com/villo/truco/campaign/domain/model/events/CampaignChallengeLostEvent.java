package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CampaignChallengeLostEvent extends CampaignDomainEvent {

  private final PlayerId rivalId;
  private final MatchId matchId;

  public CampaignChallengeLostEvent(final PlayerId playerId, final PlayerId rivalId,
      final MatchId matchId) {

    super("CAMPAIGN_CHALLENGE_LOST", playerId);
    this.rivalId = Objects.requireNonNull(rivalId);
    this.matchId = Objects.requireNonNull(matchId);
  }

  public PlayerId getRivalId() {

    return this.rivalId;
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

}
