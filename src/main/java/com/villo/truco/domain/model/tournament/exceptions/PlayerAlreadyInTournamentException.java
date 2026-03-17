package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInTournamentException extends DomainException {

  public PlayerAlreadyInTournamentException() {

    super("Player is already a participant of the tournament");
  }

}
