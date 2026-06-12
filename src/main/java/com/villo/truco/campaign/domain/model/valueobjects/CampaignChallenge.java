package com.villo.truco.campaign.domain.model.valueobjects;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record CampaignChallenge(MatchId matchId, PlayerId rivalId) {

  public CampaignChallenge {

    Objects.requireNonNull(matchId, "matchId cannot be null");
    Objects.requireNonNull(rivalId, "rivalId cannot be null");
  }

}
