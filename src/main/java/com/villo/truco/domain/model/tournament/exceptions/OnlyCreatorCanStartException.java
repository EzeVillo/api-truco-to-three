package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyCreatorCanStartException extends DomainException {

  public OnlyCreatorCanStartException() {

    super("Only the tournament creator can start the tournament");
  }

}
