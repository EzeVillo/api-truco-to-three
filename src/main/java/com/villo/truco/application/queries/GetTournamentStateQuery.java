package com.villo.truco.application.queries;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetTournamentStateQuery(TournamentId tournamentId, PlayerId requestingPlayer) {

  public GetTournamentStateQuery {

    Objects.requireNonNull(tournamentId);
    Objects.requireNonNull(requestingPlayer);
  }

  public GetTournamentStateQuery(final String tournamentId, final String requestingPlayer) {

    this(TournamentId.of(tournamentId), PlayerId.of(requestingPlayer));
  }

}
