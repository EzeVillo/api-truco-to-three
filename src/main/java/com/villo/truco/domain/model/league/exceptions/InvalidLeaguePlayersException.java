package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidLeaguePlayersException extends DomainException {

  public InvalidLeaguePlayersException(final String message) {

    super(message);
  }

}
