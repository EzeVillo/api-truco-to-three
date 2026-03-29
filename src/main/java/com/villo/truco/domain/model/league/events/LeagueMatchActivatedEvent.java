package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeagueMatchActivatedEvent extends LeagueDomainEvent {

  private final MatchId matchId;

  public LeagueMatchActivatedEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final MatchId matchId) {

    super("LEAGUE_MATCH_ACTIVATED", leagueId, participants);
    this.matchId = Objects.requireNonNull(matchId);
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

}
