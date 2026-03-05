package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record StartMatchCommand(MatchId matchId, PlayerId playerId) {

  public StartMatchCommand {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(playerId);
  }

  public StartMatchCommand(final String matchId, final String playerId) {

    this(MatchId.of(matchId), PlayerId.of(playerId));
  }

}
