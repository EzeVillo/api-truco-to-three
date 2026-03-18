package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CreateTournamentCommand(PlayerId playerId, int numberOfPlayers,
                                      GamesToPlay gamesToPlay) {

  public CreateTournamentCommand(final String playerId, final int numberOfPlayers,
      final int gamesToPlay) {

    this(PlayerId.of(playerId), numberOfPlayers, GamesToPlay.of(gamesToPlay));
  }

}
