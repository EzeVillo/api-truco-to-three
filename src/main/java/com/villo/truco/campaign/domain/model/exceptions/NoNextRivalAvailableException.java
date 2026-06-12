package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class NoNextRivalAvailableException extends DomainException {

  public NoNextRivalAvailableException() {

    super("the player is already above every bot in the ladder");
  }

}
