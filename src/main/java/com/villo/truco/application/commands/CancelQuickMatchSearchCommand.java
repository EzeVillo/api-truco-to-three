package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CancelQuickMatchSearchCommand(PlayerId playerId, String webSocketSessionId) {

  public CancelQuickMatchSearchCommand(final String playerId) {

    this(PlayerId.of(playerId), null);
  }

  public CancelQuickMatchSearchCommand(final String playerId, final String webSocketSessionId) {

    this(PlayerId.of(playerId), webSocketSessionId);
  }

}
