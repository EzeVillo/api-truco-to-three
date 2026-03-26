package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class CupStartedEvent extends DomainEventBase {

    private final CupId cupId;

    public CupStartedEvent(final CupId cupId) {

        super("CUP_STARTED");
        this.cupId = Objects.requireNonNull(cupId);
    }

    public CupId getCupId() {

        return this.cupId;
    }

}
