package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignLadderInvalidCurveInputException extends DomainException {

  public CampaignLadderInvalidCurveInputException(final int position, final int totalBots,
      final int topPoints) {

    super("invalid campaign ladder curve input: position=" + position + ", totalBots=" + totalBots
        + ", topPoints=" + topPoints);
  }

}
