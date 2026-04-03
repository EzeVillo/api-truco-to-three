package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record StopSpectatingMatchCommand(PlayerId spectatorId) {

  public StopSpectatingMatchCommand {

    Objects.requireNonNull(spectatorId);
  }

  public StopSpectatingMatchCommand(final String spectatorId) {

    this(PlayerId.of(spectatorId));
  }

}
