package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerBusyInLeagueException extends DomainException {

  public PlayerBusyInLeagueException() {

    super("Player has pending fixtures in an active league");
  }

}
