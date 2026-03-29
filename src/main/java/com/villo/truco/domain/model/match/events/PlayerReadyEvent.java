package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerReadyEvent extends MatchDomainEvent {

  private final PlayerSeat seat;

  public PlayerReadyEvent(final MatchId matchId, final PlayerId playerOne, final PlayerId playerTwo,
      final PlayerSeat seat) {

    super("PLAYER_READY", matchId, playerOne, playerTwo);
    this.seat = seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

}
