package com.villo.truco.application.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record PlayerIdentity(MatchId matchId, PlayerId playerId) {

  public PlayerIdentity {

    Objects.requireNonNull(matchId, "MatchId cannot be null");
    Objects.requireNonNull(playerId, "PlayerId cannot be null");
  }

}
