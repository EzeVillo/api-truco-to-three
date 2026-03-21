package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidCupPlayersException extends DomainException {

  public InvalidCupPlayersException(final String message) {

    super(message);
  }

}
