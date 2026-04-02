package com.villo.truco.auth.domain.model.auth.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidUserSessionRefreshException extends DomainException {

  public InvalidUserSessionRefreshException() {

    super("Refresh token is not valid for this user session");
  }

}
