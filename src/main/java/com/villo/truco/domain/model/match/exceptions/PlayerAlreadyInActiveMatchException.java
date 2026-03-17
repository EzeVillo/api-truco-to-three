package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInActiveMatchException extends DomainException {

  public PlayerAlreadyInActiveMatchException() {

    super("Player is already playing another match");
  }

}
