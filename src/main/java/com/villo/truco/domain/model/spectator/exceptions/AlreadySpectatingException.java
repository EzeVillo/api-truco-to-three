package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class AlreadySpectatingException extends DomainException {

  public AlreadySpectatingException() {

    super("Player is already spectating a match");
  }

}
