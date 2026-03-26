package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class CupFinishedEvent extends DomainEventBase {

    private final CupId cupId;
    private final PlayerId champion;

    public CupFinishedEvent(final CupId cupId, final PlayerId champion) {

        super("CUP_FINISHED");
        this.cupId = Objects.requireNonNull(cupId);
        this.champion = Objects.requireNonNull(champion);
    }

    public CupId getCupId() {

        return this.cupId;
    }

    public PlayerId getChampion() {

        return this.champion;
    }

}
