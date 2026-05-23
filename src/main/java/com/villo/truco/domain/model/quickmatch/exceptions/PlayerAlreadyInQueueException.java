package com.villo.truco.domain.model.quickmatch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerAlreadyInQueueException extends DomainException {

  public PlayerAlreadyInQueueException() {

    super("Player is already in the Quick Match queue");
  }

}
