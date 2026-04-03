package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class NotSpectatingException extends DomainException {

  public NotSpectatingException(final PlayerId playerId) {

    super("Player is not spectating any match: " + playerId);
  }

}
