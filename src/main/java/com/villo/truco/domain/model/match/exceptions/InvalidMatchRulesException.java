package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidMatchRulesException extends DomainException {

  public InvalidMatchRulesException(final String message) {

    super(message);
  }

}
