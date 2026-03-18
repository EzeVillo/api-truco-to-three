package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class LeagueFullException extends DomainException {

  public LeagueFullException() {

    super("League has reached its maximum number of players");
  }

}
