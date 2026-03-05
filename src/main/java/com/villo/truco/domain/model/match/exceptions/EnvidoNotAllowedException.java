package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class EnvidoNotAllowedException extends DomainException {

  public EnvidoNotAllowedException(final String reason) {

    super(reason);
  }

}
