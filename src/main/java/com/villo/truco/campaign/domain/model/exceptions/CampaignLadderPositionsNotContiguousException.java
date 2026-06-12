package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignLadderPositionsNotContiguousException extends DomainException {

  public CampaignLadderPositionsNotContiguousException(final int position, final int index) {

    super("campaign ladder positions must be contiguous starting at 1, but position " + position
        + " was found at index " + index);
  }

}
