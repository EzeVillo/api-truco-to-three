package com.villo.truco.domain.model.cup.exceptions;

import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public final class MatchNotPartOfCupException extends DomainException {

  public MatchNotPartOfCupException(final MatchId matchId) {

    super("Match " + matchId.value() + " is not part of this cup");
  }

}
