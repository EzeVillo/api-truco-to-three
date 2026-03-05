package com.villo.truco.application.queries;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record GetMatchStateQuery(MatchId matchId, PlayerId requestingPlayer) {

  public GetMatchStateQuery {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(requestingPlayer);
  }

  public GetMatchStateQuery(final String matchId, final String requestingPlayer) {

    this(MatchId.of(matchId), PlayerId.of(requestingPlayer));
  }

}
