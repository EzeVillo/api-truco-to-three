package com.villo.truco.application.commands;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record ForfeitCupCommand(CupId cupId, PlayerId forfeiter) {

  public ForfeitCupCommand {

    Objects.requireNonNull(cupId);
    Objects.requireNonNull(forfeiter);
  }

}
