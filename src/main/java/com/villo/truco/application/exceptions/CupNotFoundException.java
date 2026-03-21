package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;

public final class CupNotFoundException extends ApplicationException {

  public CupNotFoundException(final CupId cupId) {

    super(ApplicationStatus.NOT_FOUND, "Cup not found: " + cupId.value());
  }

  public CupNotFoundException(final InviteCode inviteCode) {

    super(ApplicationStatus.NOT_FOUND, "Cup not found for invite code: " + inviteCode.value());
  }

}
