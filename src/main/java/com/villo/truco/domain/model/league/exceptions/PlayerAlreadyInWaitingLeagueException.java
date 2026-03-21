package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInWaitingLeagueException extends DomainException {

  public PlayerAlreadyInWaitingLeagueException() {

    super("Player is already in a waiting league");
  }

}
