package com.villo.truco.domain.model.league.events;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class LeaguePlayerJoinedEvent extends DomainEventBase {

    private final LeagueId leagueId;
    private final PlayerId playerId;

    public LeaguePlayerJoinedEvent(final LeagueId leagueId, final PlayerId playerId) {

        super("LEAGUE_PLAYER_JOINED");
        this.leagueId = Objects.requireNonNull(leagueId);
        this.playerId = Objects.requireNonNull(playerId);
    }

    public LeagueId getLeagueId() {

        return this.leagueId;
    }

    public PlayerId getPlayerId() {

        return this.playerId;
    }

}
