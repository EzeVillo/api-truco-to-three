package com.villo.truco.application.commands;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinPublicCupCommand(CupId cupId, PlayerId playerId) {

  public JoinPublicCupCommand(final String cupId, final String playerId) {

    this(CupId.of(cupId), PlayerId.of(playerId));
  }

}
