package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class LeagueAdvancedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final MatchId matchId;
    private final PlayerId winner;

    public LeagueAdvancedEvent(final LeagueId leagueId, final MatchId matchId,
        final PlayerId winner) {

        super("LEAGUE_ADVANCED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.matchId = matchId;
        this.winner = Objects.requireNonNull(winner);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public MatchId getMatchId() {

        return this.matchId;
    }

    public PlayerId getWinner() {

        return this.winner;
    }

}
