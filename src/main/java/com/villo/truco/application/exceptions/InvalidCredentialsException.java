package com.villo.truco.application.exceptions;

public final class InvalidCredentialsException extends ApplicationException {

  public InvalidCredentialsException() {

    super(ApplicationStatus.UNAUTHORIZED, "Invalid credentials");
  }

}
