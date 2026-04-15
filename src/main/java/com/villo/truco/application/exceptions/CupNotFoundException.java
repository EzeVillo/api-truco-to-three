package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.JoinCode;

public final class CupNotFoundException extends ApplicationException {

  public CupNotFoundException(final CupId cupId) {

    super(ApplicationStatus.NOT_FOUND, "Cup not found: " + cupId.value());
  }

  public CupNotFoundException(final JoinCode joinCode) {

    super(ApplicationStatus.NOT_FOUND, "Cup not found for join code: " + joinCode.value());
  }

}
