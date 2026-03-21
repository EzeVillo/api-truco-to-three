package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerBusyInCupException extends DomainException {

  public PlayerBusyInCupException() {

    super("Player is still competing in an active cup");
  }

}
