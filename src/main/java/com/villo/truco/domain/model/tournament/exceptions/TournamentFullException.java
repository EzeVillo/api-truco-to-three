package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class TournamentFullException extends DomainException {

  public TournamentFullException() {

    super("Tournament has reached its maximum number of players");
  }

}
