package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record SpectateMatchCommand(MatchId matchId, PlayerId spectatorId) {

  public SpectateMatchCommand {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(spectatorId);
  }

  public SpectateMatchCommand(final String matchId, final String spectatorId) {

    this(MatchId.of(matchId), PlayerId.of(spectatorId));
  }

}
