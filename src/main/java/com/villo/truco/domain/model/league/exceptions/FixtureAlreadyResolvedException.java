package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class FixtureAlreadyResolvedException extends DomainException {

  public FixtureAlreadyResolvedException() {

    super("Fixture already resolved");
  }

}
