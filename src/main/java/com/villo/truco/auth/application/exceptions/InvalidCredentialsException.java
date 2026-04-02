package com.villo.truco.auth.application.exceptions;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;

public final class InvalidCredentialsException extends ApplicationException {

  public InvalidCredentialsException() {

    super(ApplicationStatus.UNAUTHORIZED, "Invalid credentials");
  }

}
