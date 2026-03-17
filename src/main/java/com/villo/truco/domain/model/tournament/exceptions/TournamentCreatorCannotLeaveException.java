package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class TournamentCreatorCannotLeaveException extends DomainException {

  public TournamentCreatorCannotLeaveException() {

    super("Tournament creator cannot leave the tournament");
  }

}
