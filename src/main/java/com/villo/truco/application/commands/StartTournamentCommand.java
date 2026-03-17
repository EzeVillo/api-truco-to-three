package com.villo.truco.application.commands;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record StartTournamentCommand(TournamentId tournamentId, PlayerId playerId) {

  public StartTournamentCommand {

    Objects.requireNonNull(tournamentId);
    Objects.requireNonNull(playerId);
  }

  public StartTournamentCommand(String tournamentId, String playerId) {

    this(TournamentId.of(tournamentId), PlayerId.of(playerId));
  }

}
