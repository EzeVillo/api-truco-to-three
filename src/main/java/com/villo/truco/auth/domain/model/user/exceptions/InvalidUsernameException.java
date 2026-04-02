package com.villo.truco.auth.domain.model.user.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidUsernameException extends DomainException {

  public InvalidUsernameException(final String message) {

    super(message);
  }

}
