package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class LeagueNotReadyException extends DomainException {

  public LeagueNotReadyException() {

    super("League is not ready to start");
  }

}
