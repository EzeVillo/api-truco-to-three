package com.villo.truco.application.exceptions;

public final class UsernameAlreadyTakenException extends ApplicationException {

  public UsernameAlreadyTakenException(final String username) {

    super(ApplicationStatus.UNPROCESSABLE, "Username already taken: " + username);
  }

}
