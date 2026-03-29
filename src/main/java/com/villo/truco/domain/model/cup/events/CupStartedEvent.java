package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class CupStartedEvent extends CupDomainEvent {

  public CupStartedEvent(final CupId cupId, final List<PlayerId> participants) {

    super("CUP_STARTED", cupId, participants);
  }

}
