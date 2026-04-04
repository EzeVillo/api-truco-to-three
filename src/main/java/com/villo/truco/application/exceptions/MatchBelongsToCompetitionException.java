package com.villo.truco.application.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class MatchBelongsToCompetitionException extends DomainException {

  public MatchBelongsToCompetitionException() {

    super("Cannot leave a match that belongs to a competition (cup or league)");
  }

}
