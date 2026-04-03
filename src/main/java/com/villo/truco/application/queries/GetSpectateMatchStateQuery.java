package com.villo.truco.application.queries;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetSpectateMatchStateQuery(MatchId matchId, PlayerId spectatorId) {

  public GetSpectateMatchStateQuery {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(spectatorId);
  }

  public GetSpectateMatchStateQuery(final String matchId, final String spectatorId) {

    this(MatchId.of(matchId), PlayerId.of(spectatorId));
  }

}
