package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CampaignChallengeLostEvent extends CampaignDomainEvent {

  private final PlayerId rivalId;
  private final MatchId matchId;
  private final int totalPoints;
  private final int previousPosition;
  private final int newPosition;

  public CampaignChallengeLostEvent(final PlayerId playerId, final PlayerId rivalId,
      final MatchId matchId, final int totalPoints, final int previousPosition,
      final int newPosition) {

    super("CAMPAIGN_CHALLENGE_LOST", playerId);
    this.rivalId = Objects.requireNonNull(rivalId);
    this.matchId = Objects.requireNonNull(matchId);
    this.totalPoints = totalPoints;
    this.previousPosition = previousPosition;
    this.newPosition = newPosition;
  }

  public PlayerId getRivalId() {

    return this.rivalId;
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

  public int getTotalPoints() {

    return this.totalPoints;
  }

  public int getPreviousPosition() {

    return this.previousPosition;
  }

  public int getNewPosition() {

    return this.newPosition;
  }

}
