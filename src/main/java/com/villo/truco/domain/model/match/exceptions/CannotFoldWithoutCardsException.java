package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CannotFoldWithoutCardsException extends DomainException {

  public CannotFoldWithoutCardsException() {

    super("Cannot accept truco and fold when player has no cards remaining");
  }

}
