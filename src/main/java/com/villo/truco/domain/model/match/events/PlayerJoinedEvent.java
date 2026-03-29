package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class PlayerJoinedEvent extends MatchDomainEvent {

  public PlayerJoinedEvent(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo) {

    super("PLAYER_JOINED", matchId, playerOne, playerTwo);
  }

}
