package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class LeagueCreatorCannotLeaveException extends DomainException {

  public LeagueCreatorCannotLeaveException() {

    super("League creator cannot leave the league");
  }

}
