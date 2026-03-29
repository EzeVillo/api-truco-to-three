package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class LeagueFinishedEvent extends LeagueDomainEvent {

  private final List<PlayerId> leaders;

  public LeagueFinishedEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final List<PlayerId> leaders) {

    super("LEAGUE_FINISHED", leagueId, participants);
    this.leaders = List.copyOf(leaders);
  }

  public List<PlayerId> getLeaders() {

    return this.leaders;
  }

}
