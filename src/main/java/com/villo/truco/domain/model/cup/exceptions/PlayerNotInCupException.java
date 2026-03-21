package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerNotInCupException extends DomainException {

  public PlayerNotInCupException() {

    super("Player is not in this cup");
  }

}
