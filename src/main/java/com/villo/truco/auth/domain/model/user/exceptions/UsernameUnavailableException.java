package com.villo.truco.auth.domain.model.user.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class UsernameUnavailableException extends DomainException {

  public UsernameUnavailableException(final String username) {

    super("Username already taken: " + username);
  }

}
