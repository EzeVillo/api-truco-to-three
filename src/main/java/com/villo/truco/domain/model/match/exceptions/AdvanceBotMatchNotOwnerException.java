package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class AdvanceBotMatchNotOwnerException extends DomainException {

  public AdvanceBotMatchNotOwnerException() {

    super("Only the creator of a bot-vs-bot match can advance it");
  }

}
