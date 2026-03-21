package com.villo.truco.application.commands;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record StartCupCommand(CupId cupId, PlayerId playerId) {

  public StartCupCommand {

    Objects.requireNonNull(cupId);
    Objects.requireNonNull(playerId);
  }

  public StartCupCommand(final String cupId, final String playerId) {

    this(CupId.of(cupId), PlayerId.of(playerId));
  }

}
