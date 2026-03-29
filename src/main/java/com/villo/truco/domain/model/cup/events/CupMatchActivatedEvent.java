package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class CupMatchActivatedEvent extends CupDomainEvent {

  private final MatchId matchId;

  public CupMatchActivatedEvent(final CupId cupId, final List<PlayerId> participants,
      final MatchId matchId) {

    super("CUP_MATCH_ACTIVATED", cupId, participants);
    this.matchId = Objects.requireNonNull(matchId);
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

}
