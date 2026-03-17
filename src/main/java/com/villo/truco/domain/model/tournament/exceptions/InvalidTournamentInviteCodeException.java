package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidTournamentInviteCodeException extends DomainException {

  public InvalidTournamentInviteCodeException() {

    super("Invalid tournament invite code");
  }

}
