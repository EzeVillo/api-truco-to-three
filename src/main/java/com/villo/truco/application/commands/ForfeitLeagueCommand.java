package com.villo.truco.application.commands;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record ForfeitLeagueCommand(LeagueId leagueId, PlayerId forfeiter) {

  public ForfeitLeagueCommand {

    Objects.requireNonNull(leagueId);
    Objects.requireNonNull(forfeiter);
  }

}
