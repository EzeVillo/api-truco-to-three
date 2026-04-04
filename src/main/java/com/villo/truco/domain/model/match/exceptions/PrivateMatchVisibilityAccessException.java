package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PrivateMatchVisibilityAccessException extends DomainException {

  public PrivateMatchVisibilityAccessException() {

    super("Match is private and requires invite code");
  }

}
