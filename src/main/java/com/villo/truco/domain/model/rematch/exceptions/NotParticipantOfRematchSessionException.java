package com.villo.truco.domain.model.rematch.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class NotParticipantOfRematchSessionException extends DomainException {

  public NotParticipantOfRematchSessionException() {

    super("Player is not a participant of this rematch session");
  }

}
