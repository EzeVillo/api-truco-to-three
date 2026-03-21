package com.villo.truco.application.commands;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record LeaveCupCommand(CupId cupId, PlayerId playerId) {

  public LeaveCupCommand {

    Objects.requireNonNull(cupId);
    Objects.requireNonNull(playerId);
  }

  public LeaveCupCommand(final String cupId, final String playerId) {

    this(CupId.of(cupId), PlayerId.of(playerId));
  }

}
