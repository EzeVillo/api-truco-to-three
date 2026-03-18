package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInLeagueException extends DomainException {

  public PlayerAlreadyInLeagueException() {

    super("Player is already a participant of the league");
  }

}
