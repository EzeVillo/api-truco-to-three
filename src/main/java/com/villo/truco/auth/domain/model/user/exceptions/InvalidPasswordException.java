package com.villo.truco.auth.domain.model.user.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidPasswordException extends DomainException {

  public InvalidPasswordException(final String message) {

    super(message);
  }

}
