package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public abstract class LeagueDomainEvent extends DomainEventBase {

  private final LeagueId leagueId;
  private final List<PlayerId> participants;

  protected LeagueDomainEvent(final String eventType, final LeagueId leagueId,
      final List<PlayerId> participants) {

    super(eventType);
    this.leagueId = Objects.requireNonNull(leagueId);
    this.participants = List.copyOf(Objects.requireNonNull(participants));
  }

  public LeagueId getLeagueId() {

    return leagueId;
  }

  public List<PlayerId> getParticipants() {

    return participants;
  }

}
