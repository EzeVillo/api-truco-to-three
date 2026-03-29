package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CreateBotMatchCommand(PlayerId humanPlayerId, GamesToPlay gamesToPlay,
                                    PlayerId botPlayerId) {

  public CreateBotMatchCommand(final String humanPlayerId, final int gamesToPlay,
      final String botPlayerId) {

    this(PlayerId.of(humanPlayerId), GamesToPlay.of(gamesToPlay), PlayerId.of(botPlayerId));
  }

}
