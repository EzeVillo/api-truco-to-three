package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeagueFixtureActivatedEvent extends LeagueDomainEvent {

  private final FixtureId fixtureId;

  public LeagueFixtureActivatedEvent(final LeagueId leagueId, final List<PlayerId> participants,
      final FixtureId fixtureId) {

    super("LEAGUE_FIXTURE_ACTIVATED", leagueId, participants);
    this.fixtureId = Objects.requireNonNull(fixtureId);
  }

  public FixtureId getFixtureId() {

    return this.fixtureId;
  }

}
