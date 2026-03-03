package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidInviteCodeException extends DomainException {

  public InvalidInviteCodeException() {

    super("Invalid invite code");
  }

}
