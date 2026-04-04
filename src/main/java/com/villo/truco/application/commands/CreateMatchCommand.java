package com.villo.truco.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;

public record CreateMatchCommand(PlayerId playerId, GamesToPlay gamesToPlay,
                                 Visibility visibility) {

  public CreateMatchCommand(final String playerId, final int gamesToPlay, final String visibility) {

    this(PlayerId.of(playerId), GamesToPlay.of(gamesToPlay),
        EnumArgumentParser.parse(Visibility.class, "visibility", visibility));
  }

}
