package com.villo.truco.application.exceptions;

import com.villo.truco.domain.model.cup.valueobjects.CupId;

public final class CupNotFoundException extends ApplicationException {

  public CupNotFoundException(final CupId cupId) {

    super(ApplicationStatus.NOT_FOUND, "Cup not found: " + cupId.value());
  }

}
