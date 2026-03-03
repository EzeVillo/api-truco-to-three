package com.villo.truco.application.exceptions;

public final class UnauthorizedAccessException extends ApplicationException {

  public UnauthorizedAccessException(final String message) {

    super(ApplicationStatus.UNAUTHORIZED, message);
  }

}
