package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignLadderEmptyException extends DomainException {

  public CampaignLadderEmptyException() {

    super("campaign ladder must contain at least one bot");
  }

}
