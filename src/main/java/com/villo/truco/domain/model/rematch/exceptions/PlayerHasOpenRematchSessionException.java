package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerHasOpenRematchSessionException extends DomainException {

  public PlayerHasOpenRematchSessionException() {

    super("Player has a pending rematch session");
  }

}
