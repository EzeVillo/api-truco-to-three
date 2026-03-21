package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class BoutAlreadyResolvedException extends DomainException {

  public BoutAlreadyResolvedException() {

    super("Bout is already resolved");
  }

}
