package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class LeagueFinishedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final List<PlayerId> leaders;

    public LeagueFinishedEvent(final LeagueId leagueId, final List<PlayerId> leaders) {

        super("LEAGUE_FINISHED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.leaders = Objects.requireNonNull(leaders);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public List<PlayerId> getLeaders() {

        return this.leaders;
    }

}
