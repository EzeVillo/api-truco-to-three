package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CreateMatchCommand(PlayerId playerId, GamesToPlay gamesToPlay) {

  public CreateMatchCommand(final String playerId, final int gamesToPlay) {

    this(PlayerId.of(playerId), GamesToPlay.of(gamesToPlay));
  }

}
