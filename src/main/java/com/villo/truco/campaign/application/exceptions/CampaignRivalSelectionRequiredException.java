package com.villo.truco.campaign.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class CampaignRivalSelectionRequiredException extends ApplicationException {

  public CampaignRivalSelectionRequiredException() {

    super(ApplicationStatus.BAD_REQUEST,
        "The player already reached the top of the campaign; botId is required to challenge a "
            + "specific rival");
  }

}
