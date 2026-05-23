package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CancelQuickMatchSearchCommand(PlayerId playerId) {

  public CancelQuickMatchSearchCommand(final String playerId) {

    this(PlayerId.of(playerId));
  }

}
