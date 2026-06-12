package com.villo.truco.campaign.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class CampaignRequiresRegisteredUserException extends ApplicationException {

  public CampaignRequiresRegisteredUserException() {

    super(ApplicationStatus.UNAUTHORIZED, "Campaign mode requires a registered user account");
  }

}
