package com.villo.truco.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record CallEnvidoCommand(MatchId matchId, PlayerId playerId, EnvidoCall call) {

  public CallEnvidoCommand {

    Objects.requireNonNull(matchId, "MatchId is required");
    Objects.requireNonNull(playerId, "PlayerId is required");
    Objects.requireNonNull(call, "Call is required");
  }

  public CallEnvidoCommand(final String matchId, final String playerId, final String call) {

    this(MatchId.of(matchId), PlayerId.of(playerId),
        EnumArgumentParser.parse(EnvidoCall.class, "call", call));
  }

}
