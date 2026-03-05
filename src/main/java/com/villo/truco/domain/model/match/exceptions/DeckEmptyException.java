package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class DeckEmptyException extends DomainException {

  public DeckEmptyException() {

    super("No cards remaining in the deck");
  }

}
