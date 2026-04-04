package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PrivateLeagueVisibilityAccessException extends DomainException {

  public PrivateLeagueVisibilityAccessException() {

    super("League is private and requires invite code");
  }

}
