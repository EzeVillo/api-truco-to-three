package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class MatchCancelledEvent extends MatchDomainEvent {

  public MatchCancelledEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo) {

    super("MATCH_CANCELLED", matchId, playerOne, playerTwo);
  }

}
