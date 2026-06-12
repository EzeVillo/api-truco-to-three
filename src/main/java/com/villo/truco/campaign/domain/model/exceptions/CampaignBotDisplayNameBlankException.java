package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CampaignBotDisplayNameBlankException extends DomainException {

  public CampaignBotDisplayNameBlankException() {

    super("campaign bot displayName cannot be blank");
  }

}
