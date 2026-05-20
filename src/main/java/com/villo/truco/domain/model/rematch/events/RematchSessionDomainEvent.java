package com.villo.truco.domain.model.rematch.events;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public abstract class RematchSessionDomainEvent extends DomainEventBase {

  private final RematchSessionId rematchSessionId;
  private final MatchId originMatchId;

  protected RematchSessionDomainEvent(final String eventType,
      final RematchSessionId rematchSessionId, final MatchId originMatchId) {

    super(eventType);
    this.rematchSessionId = Objects.requireNonNull(rematchSessionId);
    this.originMatchId = Objects.requireNonNull(originMatchId);
  }

  public RematchSessionId getRematchSessionId() {

    return rematchSessionId;
  }

  public MatchId getOriginMatchId() {

    return originMatchId;
  }

}
