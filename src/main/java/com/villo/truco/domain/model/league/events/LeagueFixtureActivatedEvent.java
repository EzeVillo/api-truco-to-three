package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class LeagueFixtureActivatedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final FixtureId fixtureId;

    public LeagueFixtureActivatedEvent(final LeagueId leagueId, final FixtureId fixtureId) {

        super("LEAGUE_FIXTURE_ACTIVATED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.fixtureId = Objects.requireNonNull(fixtureId);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public FixtureId getFixtureId() {

        return this.fixtureId;
    }

}
