package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class LeaguePlayerForfeitedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final PlayerId forfeiter;

    public LeaguePlayerForfeitedEvent(final LeagueId leagueId, final PlayerId forfeiter) {

        super("LEAGUE_PLAYER_FORFEITED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.forfeiter = Objects.requireNonNull(forfeiter);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public PlayerId getForfeiter() {

        return this.forfeiter;
    }

}
