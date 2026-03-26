package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class LeagueMatchActivatedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final MatchId matchId;

    public LeagueMatchActivatedEvent(final LeagueId leagueId, final MatchId matchId) {

        super("LEAGUE_MATCH_ACTIVATED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.matchId = Objects.requireNonNull(matchId);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public MatchId getMatchId() {

        return this.matchId;
    }

}
