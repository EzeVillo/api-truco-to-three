package com.villo.truco.domain.model.chat.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerNotInChatException extends DomainException {

  public PlayerNotInChatException(final PlayerId playerId) {

    super("Player not in chat: " + playerId.value());
  }

}
