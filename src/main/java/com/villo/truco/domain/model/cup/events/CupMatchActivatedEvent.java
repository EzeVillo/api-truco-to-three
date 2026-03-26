package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class CupMatchActivatedEvent extends DomainEventBase {

    private final CupId cupId;
    private final MatchId matchId;

    public CupMatchActivatedEvent(final CupId cupId, final MatchId matchId) {

        super("CUP_MATCH_ACTIVATED");
        this.cupId = Objects.requireNonNull(cupId);
        this.matchId = Objects.requireNonNull(matchId);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public MatchId getMatchId() {

        return this.matchId;
    }

}
