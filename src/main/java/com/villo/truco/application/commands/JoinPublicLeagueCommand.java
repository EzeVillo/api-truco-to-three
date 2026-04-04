package com.villo.truco.application.commands;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinPublicLeagueCommand(LeagueId leagueId, PlayerId playerId) {

  public JoinPublicLeagueCommand(final String leagueId, final String playerId) {

    this(LeagueId.of(leagueId), PlayerId.of(playerId));
  }

}
