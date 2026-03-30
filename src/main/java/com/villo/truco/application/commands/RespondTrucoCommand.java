package com.villo.truco.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record RespondTrucoCommand(MatchId matchId, PlayerId playerId, TrucoResponse response) {

  public RespondTrucoCommand {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(response);
  }

  public RespondTrucoCommand(final String matchId, final String playerId, final String response) {

    this(MatchId.of(matchId), PlayerId.of(playerId),
        EnumArgumentParser.parse(TrucoResponse.class, "response", response));

  }

}
