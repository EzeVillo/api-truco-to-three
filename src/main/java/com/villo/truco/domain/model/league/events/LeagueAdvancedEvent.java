package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeagueAdvancedEvent extends LeagueDomainEvent {

  private final MatchId matchId;
  private final PlayerId winner;

  public LeagueAdvancedEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final MatchId matchId, final PlayerId winner) {

    super("LEAGUE_ADVANCED", leagueId, participants);
    this.matchId = matchId;
    this.winner = Objects.requireNonNull(winner);
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

  public PlayerId getWinner() {

    return this.winner;
  }

}
