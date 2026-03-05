package com.villo.truco.domain.model.match.valueobjects.exceptions;

import com.villo.truco.domain.shared.DomainException;

public class InvalidCardException extends DomainException {

  public InvalidCardException(final int number) {

    super("Invalid card number: " + number);
  }

}
