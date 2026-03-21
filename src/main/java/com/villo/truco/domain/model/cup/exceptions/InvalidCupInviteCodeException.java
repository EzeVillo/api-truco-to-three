package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidCupInviteCodeException extends DomainException {

  public InvalidCupInviteCodeException() {

    super("Invalid cup invite code");
  }

}
