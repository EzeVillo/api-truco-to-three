package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidJoinCodeException extends DomainException {

  public InvalidJoinCodeException() {

    super("Invalid invite code");
  }

}
