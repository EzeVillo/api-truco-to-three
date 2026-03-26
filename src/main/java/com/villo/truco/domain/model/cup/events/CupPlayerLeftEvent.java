package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CupPlayerLeftEvent extends DomainEventBase {

    private final CupId cupId;
    private final PlayerId playerId;

    public CupPlayerLeftEvent(final CupId cupId, final PlayerId playerId) {

        super("CUP_PLAYER_LEFT");
        this.cupId = Objects.requireNonNull(cupId);
        this.playerId = Objects.requireNonNull(playerId);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public PlayerId getPlayerId() {

        return this.playerId;
    }

}
