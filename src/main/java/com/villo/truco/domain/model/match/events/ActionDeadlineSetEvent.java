package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class ActionDeadlineSetEvent extends MatchDomainEvent implements MatchDerivedEvent {

  private final PlayerSeat seat;

  public ActionDeadlineSetEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final PlayerSeat seat) {

    super("ACTION_DEADLINE_SET", matchId, playerOne, playerTwo);
    this.seat = seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

}
