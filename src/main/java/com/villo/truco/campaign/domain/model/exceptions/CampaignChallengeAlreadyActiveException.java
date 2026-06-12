package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class CampaignChallengeAlreadyActiveException extends DomainException {

  public CampaignChallengeAlreadyActiveException(final PlayerId playerId, final MatchId matchId) {

    super("player " + playerId.value() + " already has an active campaign challenge in match "
        + matchId.value());
  }

}
