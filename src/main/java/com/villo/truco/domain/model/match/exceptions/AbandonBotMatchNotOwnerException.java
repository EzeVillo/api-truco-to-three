package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class AbandonBotMatchNotOwnerException extends DomainException {

  public AbandonBotMatchNotOwnerException() {

    super("Only the creator of a bot-vs-bot match can abandon it");
  }

}
