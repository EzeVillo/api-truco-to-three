package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class NotYourTurnException extends DomainException {

  public NotYourTurnException(final PlayerId playerId) {

    super("It is not the turn of player: " + playerId);
  }

}
