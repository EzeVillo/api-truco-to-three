package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class BotCannotLeaveRematchSessionException extends DomainException {

  public BotCannotLeaveRematchSessionException() {

    super("Bot cannot leave a rematch session");
  }

}
