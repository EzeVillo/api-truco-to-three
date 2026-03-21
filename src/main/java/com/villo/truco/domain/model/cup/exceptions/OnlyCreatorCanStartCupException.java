package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyCreatorCanStartCupException extends DomainException {

  public OnlyCreatorCanStartCupException() {

    super("Only the cup creator can start the cup");
  }

}
