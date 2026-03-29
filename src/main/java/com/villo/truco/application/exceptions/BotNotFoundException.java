package com.villo.truco.application.exceptions;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class BotNotFoundException extends ApplicationException {

  public BotNotFoundException(final PlayerId botId) {

    super(ApplicationStatus.NOT_FOUND, "Bot not found: " + botId);
  }

}
