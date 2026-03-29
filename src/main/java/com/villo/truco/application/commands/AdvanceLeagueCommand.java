package com.villo.truco.application.commands;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AdvanceLeagueCommand(LeagueId leagueId, MatchId matchId, PlayerId winner) {

  public AdvanceLeagueCommand {

    Objects.requireNonNull(leagueId);
    Objects.requireNonNull(matchId);
    Objects.requireNonNull(winner);
  }

}
