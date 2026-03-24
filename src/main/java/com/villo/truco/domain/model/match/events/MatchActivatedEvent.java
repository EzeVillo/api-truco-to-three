package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class MatchActivatedEvent extends DomainEventBase {

    private final MatchId matchId;

    public MatchActivatedEvent(final MatchId matchId) {

        super("MATCH_ACTIVATED");
        this.matchId = Objects.requireNonNull(matchId);
    }

    public MatchId getMatchId() {

        return this.matchId;
    }

}
