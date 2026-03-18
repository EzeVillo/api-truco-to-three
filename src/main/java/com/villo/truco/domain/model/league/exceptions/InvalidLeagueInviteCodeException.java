package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidLeagueInviteCodeException extends DomainException {

  public InvalidLeagueInviteCodeException() {

    super("Invalid league invite code");
  }

}
