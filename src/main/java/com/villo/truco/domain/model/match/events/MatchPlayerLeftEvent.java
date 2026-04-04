package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class MatchPlayerLeftEvent extends MatchDomainEvent {

  private final PlayerSeat leaverSeat;

  public MatchPlayerLeftEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerSeat leaverSeat) {

    super("MATCH_PLAYER_LEFT", matchId, playerOne, null);
    this.leaverSeat = Objects.requireNonNull(leaverSeat);
  }

  public PlayerSeat getLeaverSeat() {

    return leaverSeat;
  }

}
