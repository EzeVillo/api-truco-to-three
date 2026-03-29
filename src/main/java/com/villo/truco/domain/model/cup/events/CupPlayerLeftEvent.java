package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupPlayerLeftEvent extends CupDomainEvent {

  private final PlayerId playerId;

  public CupPlayerLeftEvent(final CupId cupId, final List<PlayerId> participants,
      final PlayerId playerId) {

    super("CUP_PLAYER_LEFT", cupId, participants);
    this.playerId = Objects.requireNonNull(playerId);
  }

  public PlayerId getPlayerId() {

    return this.playerId;
  }

}
