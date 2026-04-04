package com.villo.truco.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;

public record CreateCupCommand(PlayerId playerId, int numberOfPlayers, GamesToPlay gamesToPlay,
                               Visibility visibility) {

  public CreateCupCommand(final String playerId, final int numberOfPlayers, final int gamesToPlay,
      final String visibility) {

    this(PlayerId.of(playerId), numberOfPlayers, GamesToPlay.of(gamesToPlay),
        EnumArgumentParser.parse(Visibility.class, "visibility", visibility));
  }

}
