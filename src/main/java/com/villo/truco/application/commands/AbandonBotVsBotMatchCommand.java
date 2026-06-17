package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AbandonBotVsBotMatchCommand(MatchId matchId, PlayerId ownerId) {

  public AbandonBotVsBotMatchCommand {

    Objects.requireNonNull(matchId, "MatchId is required");
    Objects.requireNonNull(ownerId, "OwnerId is required");
  }

  public AbandonBotVsBotMatchCommand(final String matchId, final String ownerId) {

    this(MatchId.of(matchId), PlayerId.of(ownerId));
  }

}
