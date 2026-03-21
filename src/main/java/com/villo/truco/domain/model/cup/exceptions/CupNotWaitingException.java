package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CupNotWaitingException extends DomainException {

  public CupNotWaitingException() {

    super("Cup is not waiting for players");
  }

}
