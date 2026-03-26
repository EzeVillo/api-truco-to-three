package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class CupCancelledEvent extends DomainEventBase {

    private final CupId cupId;

    public CupCancelledEvent(final CupId cupId) {

        super("CUP_CANCELLED");
        this.cupId = Objects.requireNonNull(cupId);
    }

    public CupId getCupId() {

        return this.cupId;
    }

}
