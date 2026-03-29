package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AbandonMatchCommand(MatchId matchId, PlayerId playerId) {

  public AbandonMatchCommand {

    Objects.requireNonNull(matchId, "MatchId is required");
    Objects.requireNonNull(playerId, "PlayerId is required");
  }

  public AbandonMatchCommand(final String matchId, final String playerId) {

    this(MatchId.of(matchId), PlayerId.of(playerId));
  }

}
