package com.villo.truco.infrastructure.persistence.exceptions;

import com.villo.truco.domain.shared.exceptions.RetriableException;

public class StaleAggregateException extends RetriableException {

  public StaleAggregateException(final String message, final Throwable cause) {

    super(message, cause);
  }

}
