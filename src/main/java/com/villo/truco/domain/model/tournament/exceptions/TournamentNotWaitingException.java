package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class TournamentNotWaitingException extends DomainException {

  public TournamentNotWaitingException() {

    super("Tournament is not waiting for players");
  }

}
