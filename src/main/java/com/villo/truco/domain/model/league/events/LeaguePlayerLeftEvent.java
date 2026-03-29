package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeaguePlayerLeftEvent extends LeagueDomainEvent {

  private final PlayerId playerId;

  public LeaguePlayerLeftEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final PlayerId playerId) {

    super("LEAGUE_PLAYER_LEFT", leagueId, participants);
    this.playerId = Objects.requireNonNull(playerId);
  }

  public PlayerId getPlayerId() {

    return this.playerId;
  }

}
