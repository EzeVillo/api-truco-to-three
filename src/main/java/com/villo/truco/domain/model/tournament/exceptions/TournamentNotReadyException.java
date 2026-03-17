package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class TournamentNotReadyException extends DomainException {

  public TournamentNotReadyException() {

    super("Tournament is not ready to start");
  }

}
