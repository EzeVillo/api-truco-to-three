package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerNotInMatchException extends DomainException {

  public PlayerNotInMatchException(final PlayerId playerId) {

    super("Player not in match: " + playerId.value());
  }

}
