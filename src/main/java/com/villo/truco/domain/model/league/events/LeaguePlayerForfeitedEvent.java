package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeaguePlayerForfeitedEvent extends LeagueDomainEvent {

  private final PlayerId forfeiter;

  public LeaguePlayerForfeitedEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final PlayerId forfeiter) {

    super("LEAGUE_PLAYER_FORFEITED", leagueId, participants);
    this.forfeiter = Objects.requireNonNull(forfeiter);
  }

  public PlayerId getForfeiter() {

    return this.forfeiter;
  }

}
