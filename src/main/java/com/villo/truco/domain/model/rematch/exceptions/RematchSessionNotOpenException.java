package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class RematchSessionNotOpenException extends DomainException {

  public RematchSessionNotOpenException() {

    super("Rematch session is no longer open");
  }

}
