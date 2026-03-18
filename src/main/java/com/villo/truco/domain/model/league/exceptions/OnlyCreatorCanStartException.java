package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyCreatorCanStartException extends DomainException {

  public OnlyCreatorCanStartException() {

    super("Only the league creator can start the league");
  }

}
