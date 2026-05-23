package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record EnqueueForQuickMatchCommand(PlayerId playerId, GamesToPlay gamesToPlay,
                                          String webSocketSessionId) {

  public EnqueueForQuickMatchCommand(final String playerId, final int gamesToPlay,
      final String webSocketSessionId) {

    this(PlayerId.of(playerId), GamesToPlay.of(gamesToPlay), webSocketSessionId);
  }

}
