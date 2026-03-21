package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInWaitingCupException extends DomainException {

  public PlayerAlreadyInWaitingCupException() {

    super("Player is already in a waiting cup");
  }

}
