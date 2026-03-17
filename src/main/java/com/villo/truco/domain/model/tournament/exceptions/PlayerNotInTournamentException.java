package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerNotInTournamentException extends DomainException {

  public PlayerNotInTournamentException() {

    super("Player is not a participant of the tournament");
  }

}
