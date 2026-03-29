package com.villo.truco.domain.model.bot.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PendingEnvidoCallRequiredException extends DomainException {

  public PendingEnvidoCallRequiredException() {

    super("Cannot decide envido response without a pending envido call");
  }

}
