package com.villo.truco.application.queries;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record GetCupStateQuery(CupId cupId, PlayerId requestingPlayer) {

  public GetCupStateQuery(final String cupId, final String playerId) {

    this(CupId.of(cupId), PlayerId.of(playerId));
  }

}
