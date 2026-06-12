package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class NoActiveCampaignChallengeException extends DomainException {

  public NoActiveCampaignChallengeException(final PlayerId playerId, final MatchId matchId) {

    super("player " + playerId.value() + " has no active campaign challenge for match "
        + matchId.value());
  }

}
