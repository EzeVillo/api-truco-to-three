package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupPlayerJoinedEvent extends CupDomainEvent {

  private final PlayerId playerId;

  public CupPlayerJoinedEvent(final CupId cupId, final List<PlayerId> participants,
      final PlayerId playerId) {

    super("CUP_PLAYER_JOINED", cupId, participants);
    this.playerId = Objects.requireNonNull(playerId);
  }

  public PlayerId getPlayerId() {

    return this.playerId;
  }

}
