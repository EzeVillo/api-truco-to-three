package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.Objects;

public final class CupBoutActivatedEvent extends DomainEventBase {

    private final CupId cupId;
    private final BoutId boutId;

    public CupBoutActivatedEvent(final CupId cupId, final BoutId boutId) {

        super("CUP_BOUT_ACTIVATED");
        this.cupId = Objects.requireNonNull(cupId);
        this.boutId = Objects.requireNonNull(boutId);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public BoutId getBoutId() {

        return this.boutId;
    }

}
