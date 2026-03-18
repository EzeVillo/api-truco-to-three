package com.villo.truco.application.queries;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetLeagueStateQuery(LeagueId leagueId, PlayerId requestingPlayer) {

  public GetLeagueStateQuery {

    Objects.requireNonNull(leagueId);
    Objects.requireNonNull(requestingPlayer);
  }

  public GetLeagueStateQuery(final String leagueId, final String requestingPlayer) {

    this(LeagueId.of(leagueId), PlayerId.of(requestingPlayer));
  }

}
