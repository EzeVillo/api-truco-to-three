package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import java.util.Objects;

public record RegisterTournamentMatchResultCommand(TournamentId tournamentId, MatchId matchId) {

  public RegisterTournamentMatchResultCommand {

    Objects.requireNonNull(tournamentId);
    Objects.requireNonNull(matchId);
  }

  public RegisterTournamentMatchResultCommand(final String tournamentId, final String matchId) {

    this(TournamentId.of(tournamentId), MatchId.of(matchId));
  }

}
