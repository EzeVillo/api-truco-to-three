package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class MatchNotFullException extends DomainException {

  public MatchNotFullException() {

    super("Cannot start match: waiting for opponent to join");
  }

}
