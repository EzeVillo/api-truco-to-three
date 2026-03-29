package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class LeagueStartedEvent extends LeagueDomainEvent {

  public LeagueStartedEvent(final LeagueId leagueId, final List<PlayerId> participants) {

    super("LEAGUE_STARTED", leagueId, participants);
  }

}
