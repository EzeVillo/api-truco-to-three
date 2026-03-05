package com.villo.truco.application.exceptions;

public abstract class ApplicationException extends RuntimeException {

  private final ApplicationStatus status;

  protected ApplicationException(final ApplicationStatus status, final String message) {

    super(message);
    this.status = status;
  }

  public ApplicationStatus getStatus() {

    return this.status;
  }

}
