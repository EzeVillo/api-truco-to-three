package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInCupException extends DomainException {

  public PlayerAlreadyInCupException() {

    super("Player is already in this cup");
  }

}
