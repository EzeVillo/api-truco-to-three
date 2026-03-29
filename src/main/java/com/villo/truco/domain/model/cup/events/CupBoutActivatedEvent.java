package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupBoutActivatedEvent extends CupDomainEvent {

  private final BoutId boutId;

  public CupBoutActivatedEvent(final CupId cupId, final List<PlayerId> participants,
      final BoutId boutId) {

    super("CUP_BOUT_ACTIVATED", cupId, participants);
    this.boutId = Objects.requireNonNull(boutId);
  }

  public BoutId getBoutId() {

    return this.boutId;
  }

}
