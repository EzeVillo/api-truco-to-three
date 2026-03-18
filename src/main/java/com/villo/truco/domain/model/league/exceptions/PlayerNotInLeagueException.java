package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerNotInLeagueException extends DomainException {

  public PlayerNotInLeagueException() {

    super("Player is not a participant of the league");
  }

}
