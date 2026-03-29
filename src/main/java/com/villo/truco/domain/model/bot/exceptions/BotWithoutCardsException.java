package com.villo.truco.domain.model.bot.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class BotWithoutCardsException extends DomainException {

  public BotWithoutCardsException() {

    super("Bot has no cards to play");
  }

}
