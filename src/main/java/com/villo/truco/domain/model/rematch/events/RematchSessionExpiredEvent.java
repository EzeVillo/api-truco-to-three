package com.villo.truco.domain.model.rematch.events;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class RematchSessionExpiredEvent extends RematchSessionDomainEvent {

  private final PlayerId playerOneId;
  private final PlayerId playerTwoId;

  public RematchSessionExpiredEvent(final RematchSessionId rematchSessionId,
      final MatchId originMatchId, final PlayerId playerOneId, final PlayerId playerTwoId) {

    super("REMATCH_EXPIRED", rematchSessionId, originMatchId);
    this.playerOneId = Objects.requireNonNull(playerOneId);
    this.playerTwoId = Objects.requireNonNull(playerTwoId);
  }

  public PlayerId getPlayerOneId() {

    return playerOneId;
  }

  public PlayerId getPlayerTwoId() {

    return playerTwoId;
  }

}
