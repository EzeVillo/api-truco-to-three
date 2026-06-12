package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class BotNotImmediatelyAboveException extends DomainException {

  public BotNotImmediatelyAboveException(final PlayerId botId) {

    super("bot " + botId.value()
        + " cannot be challenged: only the bot immediately above can be challenged until top 1 is reached");
  }

}
