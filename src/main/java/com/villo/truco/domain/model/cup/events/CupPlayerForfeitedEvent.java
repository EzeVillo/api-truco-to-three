package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CupPlayerForfeitedEvent extends DomainEventBase {

    private final CupId cupId;
    private final PlayerId forfeiter;

    public CupPlayerForfeitedEvent(final CupId cupId, final PlayerId forfeiter) {

        super("CUP_PLAYER_FORFEITED");
        this.cupId = Objects.requireNonNull(cupId);
        this.forfeiter = Objects.requireNonNull(forfeiter);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public PlayerId getForfeiter() {

        return this.forfeiter;
    }

}
