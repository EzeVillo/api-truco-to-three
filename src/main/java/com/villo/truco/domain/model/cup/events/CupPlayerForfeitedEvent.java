package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupPlayerForfeitedEvent extends CupDomainEvent {

  private final PlayerId forfeiter;

  public CupPlayerForfeitedEvent(final CupId cupId, final List<PlayerId> participants,
      final PlayerId forfeiter) {

    super("CUP_PLAYER_FORFEITED", cupId, participants);
    this.forfeiter = Objects.requireNonNull(forfeiter);
  }

  public PlayerId getForfeiter() {

    return this.forfeiter;
  }

}
