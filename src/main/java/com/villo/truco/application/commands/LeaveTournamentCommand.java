package com.villo.truco.application.commands;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record LeaveTournamentCommand(TournamentId tournamentId, PlayerId playerId) {

  public LeaveTournamentCommand {

    Objects.requireNonNull(tournamentId);
    Objects.requireNonNull(playerId);
  }

  public LeaveTournamentCommand(String tournamentId, String playerId) {

    this(TournamentId.of(tournamentId), PlayerId.of(playerId));
  }

}
