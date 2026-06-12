package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidCampaignRivalRecordException extends DomainException {

  public InvalidCampaignRivalRecordException(final int wins, final int losses) {

    super(
        "campaign rival record cannot hold negative counters: wins=" + wins + ", losses=" + losses);
  }

}
