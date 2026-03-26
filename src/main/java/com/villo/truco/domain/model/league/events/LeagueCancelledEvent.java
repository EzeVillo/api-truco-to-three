package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class LeagueCancelledEvent extends DomainEventBase {

    private final LeagueId leagueId;

    public LeagueCancelledEvent(final LeagueId leagueId) {

        super("LEAGUE_CANCELLED");
        this.leagueId = Objects.requireNonNull(leagueId);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

}
