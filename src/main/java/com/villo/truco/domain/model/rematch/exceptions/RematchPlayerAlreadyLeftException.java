package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class RematchPlayerAlreadyLeftException extends DomainException {

  public RematchPlayerAlreadyLeftException() {

    super("Player already left the rematch session");
  }

}
