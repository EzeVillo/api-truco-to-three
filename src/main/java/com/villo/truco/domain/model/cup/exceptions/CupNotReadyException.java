package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CupNotReadyException extends DomainException {

  public CupNotReadyException() {

    super("Cup is not ready to start");
  }

}
