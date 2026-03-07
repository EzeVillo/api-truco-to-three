package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class FoldNotAllowedException extends DomainException {

  public FoldNotAllowedException(final String reason) {

    super(reason);
  }

}
