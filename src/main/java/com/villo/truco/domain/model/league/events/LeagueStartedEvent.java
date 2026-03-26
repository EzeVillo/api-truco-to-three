package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class LeagueStartedEvent extends DomainEventBase {

    private final LeagueId leagueId;

    public LeagueStartedEvent(final LeagueId leagueId) {

        super("LEAGUE_STARTED");
        this.leagueId = Objects.requireNonNull(leagueId);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

}
