package com.villo.truco.application.exceptions;

public final class InvalidCursorPageRequestException extends ApplicationException {

  public InvalidCursorPageRequestException(final String message) {

    super(ApplicationStatus.BAD_REQUEST, message);
  }

}
