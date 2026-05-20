package com.villo.truco.domain.model.rematch.events;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class RematchPlayerWantsRematchEvent extends RematchSessionDomainEvent {

  private final PlayerId actorId;
  private final PlayerId otherPlayerId;

  public RematchPlayerWantsRematchEvent(final RematchSessionId rematchSessionId,
      final MatchId originMatchId, final PlayerId actorId, final PlayerId otherPlayerId) {

    super("REMATCH_OPPONENT_WANTS", rematchSessionId, originMatchId);
    this.actorId = Objects.requireNonNull(actorId);
    this.otherPlayerId = Objects.requireNonNull(otherPlayerId);
  }

  public PlayerId getActorId() {

    return actorId;
  }

  public PlayerId getOtherPlayerId() {

    return otherPlayerId;
  }

}
