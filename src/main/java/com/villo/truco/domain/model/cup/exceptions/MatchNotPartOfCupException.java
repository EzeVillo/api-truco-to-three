package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainException;

public final class MatchNotPartOfCupException extends DomainException {

  public MatchNotPartOfCupException(final MatchId matchId) {

    super("Match " + matchId.value() + " is not part of this cup");
  }

}
