package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class PlayerOwnsActiveBotMatchException extends DomainException {

  public PlayerOwnsActiveBotMatchException() {

    super("Player already owns an active bot-vs-bot match");
  }

}
