package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CupFullException extends DomainException {

  public CupFullException() {

    super("Cup has reached its maximum number of players");
  }

}
