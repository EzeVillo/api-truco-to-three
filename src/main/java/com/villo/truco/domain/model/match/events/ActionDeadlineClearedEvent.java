package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class ActionDeadlineClearedEvent extends MatchDomainEvent implements
    MatchDerivedEvent {

  public ActionDeadlineClearedEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo) {

    super("ACTION_DEADLINE_CLEARED", matchId, playerOne, playerTwo);
  }

}
