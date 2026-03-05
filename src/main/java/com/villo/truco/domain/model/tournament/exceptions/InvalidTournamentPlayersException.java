package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidTournamentPlayersException extends DomainException {

  public InvalidTournamentPlayersException(final String message) {

    super(message);
  }

}
