package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class WinnerNotInBoutException extends DomainException {

  public WinnerNotInBoutException() {

    super("Winner is not a player in this bout");
  }

}
