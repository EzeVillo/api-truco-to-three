package com.villo.truco.domain.model.league.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainException;

public final class MatchNotPartOfLeagueException extends DomainException {

  public MatchNotPartOfLeagueException(final MatchId matchId) {

    super("Match is not part of league: " + matchId.value());
  }

}
