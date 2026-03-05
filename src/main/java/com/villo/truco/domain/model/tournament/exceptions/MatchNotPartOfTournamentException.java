package com.villo.truco.domain.model.tournament.exceptions;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainException;

public final class MatchNotPartOfTournamentException extends DomainException {

  public MatchNotPartOfTournamentException(final MatchId matchId) {

    super("Match is not part of tournament: " + matchId.value());
  }

}
