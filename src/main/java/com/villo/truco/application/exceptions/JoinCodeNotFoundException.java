package com.villo.truco.application.exceptions;

import com.villo.truco.domain.shared.valueobjects.JoinCode;

public final class JoinCodeNotFoundException extends ApplicationException {

  public JoinCodeNotFoundException(final JoinCode joinCode) {

    super(ApplicationStatus.NOT_FOUND, "Join code not found: " + joinCode.value());
  }

}
