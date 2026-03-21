package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class CupCreatorCannotLeaveException extends DomainException {

  public CupCreatorCannotLeaveException() {

    super("Cup creator cannot leave the cup");
  }

}
