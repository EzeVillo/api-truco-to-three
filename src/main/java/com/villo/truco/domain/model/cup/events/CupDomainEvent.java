package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public abstract class CupDomainEvent extends DomainEventBase {

  private final CupId cupId;
  private final List<PlayerId> participants;

  protected CupDomainEvent(final String eventType, final CupId cupId,
      final List<PlayerId> participants) {

    super(eventType);
    this.cupId = Objects.requireNonNull(cupId);
    this.participants = List.copyOf(Objects.requireNonNull(participants));
  }

  public CupId getCupId() {

    return cupId;
  }

  public List<PlayerId> getParticipants() {

    return participants;
  }

}
