package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CupAdvancedEvent extends DomainEventBase {

    private final CupId cupId;
    private final MatchId matchId;
    private final PlayerId winner;

    public CupAdvancedEvent(final CupId cupId, final MatchId matchId, final PlayerId winner) {

        super("CUP_ADVANCED");
        this.cupId = Objects.requireNonNull(cupId);
        this.matchId = matchId;
        this.winner = Objects.requireNonNull(winner);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public MatchId getMatchId() {

        return this.matchId;
    }

    public PlayerId getWinner() {

        return this.winner;
    }

}
