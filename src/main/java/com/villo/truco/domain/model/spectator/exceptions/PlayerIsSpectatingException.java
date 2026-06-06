package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerIsSpectatingException extends DomainException {

  public PlayerIsSpectatingException() {

    super("Player is currently spectating a match");
  }

}
