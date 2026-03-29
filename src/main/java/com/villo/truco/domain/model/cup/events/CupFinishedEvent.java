package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupFinishedEvent extends CupDomainEvent {

  private final PlayerId champion;

  public CupFinishedEvent(final CupId cupId, final List<PlayerId> participants,
      final PlayerId champion) {

    super("CUP_FINISHED", cupId, participants);
    this.champion = Objects.requireNonNull(champion);
  }

  public PlayerId getChampion() {

    return this.champion;
  }

}
