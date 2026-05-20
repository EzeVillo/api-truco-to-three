package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class RematchSessionExpiredException extends DomainException {

  public RematchSessionExpiredException() {

    super("Rematch session has expired");
  }

}
