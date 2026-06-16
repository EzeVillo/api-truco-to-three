package com.villo.truco.domain.model.spectator.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class SpectateBotMatchNotOwnerException extends DomainException {

  public SpectateBotMatchNotOwnerException() {

    super("Only the creator of a bot-vs-bot match can spectate it");
  }

}
