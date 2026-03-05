package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record FoldCommand(MatchId matchId, PlayerId playerId) {

  public FoldCommand {

    Objects.requireNonNull(matchId, "MatchId is required");
    Objects.requireNonNull(playerId, "PlayerId is required");
  }

  public FoldCommand(final String matchId, final String playerId) {

    this(MatchId.of(matchId), PlayerId.of(playerId));
  }

}
