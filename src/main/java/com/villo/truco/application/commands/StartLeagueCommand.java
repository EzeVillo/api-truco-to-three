package com.villo.truco.application.commands;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record StartLeagueCommand(LeagueId leagueId, PlayerId playerId) {

  public StartLeagueCommand {

    Objects.requireNonNull(leagueId);
    Objects.requireNonNull(playerId);
  }

  public StartLeagueCommand(String leagueId, String playerId) {

    this(LeagueId.of(leagueId), PlayerId.of(playerId));
  }

}
