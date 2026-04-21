package com.villo.truco.domain.shared.exceptions;

public abstract class RetriableException extends RuntimeException {

  protected RetriableException(final String message) {

    super(message);
  }

  protected RetriableException(final String message, final Throwable cause) {

    super(message, cause);
  }

}
