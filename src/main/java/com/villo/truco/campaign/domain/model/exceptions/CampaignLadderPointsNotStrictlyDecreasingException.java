package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignLadderPointsNotStrictlyDecreasingException extends DomainException {

  public CampaignLadderPointsNotStrictlyDecreasingException(final int position,
      final int previousPosition) {

    super("campaign ladder points must be strictly decreasing, but position " + position
        + " does not have fewer points than position " + previousPosition);
  }

}
