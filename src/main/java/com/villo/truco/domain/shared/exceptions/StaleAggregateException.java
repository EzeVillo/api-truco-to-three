package com.villo.truco.domain.shared.exceptions;

public class StaleAggregateException extends RuntimeException {

  public StaleAggregateException(final String message, final Throwable cause) {

    super(message, cause);
  }

}
