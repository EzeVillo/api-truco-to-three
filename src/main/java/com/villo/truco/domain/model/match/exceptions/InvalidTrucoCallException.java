package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidTrucoCallException extends DomainException {

  public InvalidTrucoCallException() {

    super("No se puede cantar truco en este momento");
  }

}
