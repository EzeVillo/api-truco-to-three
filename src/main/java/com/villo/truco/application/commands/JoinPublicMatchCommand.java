package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinPublicMatchCommand(MatchId matchId, PlayerId playerId) {

  public JoinPublicMatchCommand(final String matchId, final String playerId) {

    this(MatchId.of(matchId), PlayerId.of(playerId));
  }

}
