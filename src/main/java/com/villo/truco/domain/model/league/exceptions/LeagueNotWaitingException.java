package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class LeagueNotWaitingException extends DomainException {

  public LeagueNotWaitingException() {

    super("League is not waiting for players");
  }

}
