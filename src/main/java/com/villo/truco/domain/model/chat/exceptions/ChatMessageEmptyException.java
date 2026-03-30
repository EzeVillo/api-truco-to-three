package com.villo.truco.domain.model.chat.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class ChatMessageEmptyException extends DomainException {

  public ChatMessageEmptyException() {

    super("Message content cannot be empty");
  }

}
