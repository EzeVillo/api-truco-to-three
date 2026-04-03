package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CannotSpectateOwnMatchException extends DomainException {

  public CannotSpectateOwnMatchException() {

    super("Cannot spectate a match you are playing in");
  }

}
