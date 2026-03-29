package com.villo.truco.domain.model.bot.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidBotPersonalityException extends DomainException {

  public InvalidBotPersonalityException(final String name, final int value) {

    super(name + " must be between 1 and 100, got " + value);
  }

}
