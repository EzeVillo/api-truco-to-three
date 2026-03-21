package com.villo.truco.domain.model.cup.exceptions;

public final class BracketCorruptedException extends IllegalStateException {

  public BracketCorruptedException(final int roundNumber, final int bracketPosition) {

    super("Bout not found in bracket: round=" + roundNumber + ", position=" + bracketPosition);
  }

}
