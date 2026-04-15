package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinByCodeCommand(PlayerId playerId, JoinCode joinCode) {

  public JoinByCodeCommand(final String playerId, final String joinCode) {

    this(PlayerId.of(playerId), JoinCode.of(joinCode));
  }

}
