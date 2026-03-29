package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class MatchEventEnvelope extends MatchDomainEvent {

  private final DomainEventBase inner;

  public MatchEventEnvelope(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final DomainEventBase inner) {

    super(Objects.requireNonNull(inner).getEventType(), matchId, playerOne, playerTwo);
    this.inner = inner;
  }

  public DomainEventBase getInner() {

    return this.inner;
  }

}
