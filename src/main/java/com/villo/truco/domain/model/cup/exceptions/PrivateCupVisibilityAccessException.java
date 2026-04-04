package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PrivateCupVisibilityAccessException extends DomainException {

  public PrivateCupVisibilityAccessException() {

    super("Cup is private and requires invite code");
  }

}
