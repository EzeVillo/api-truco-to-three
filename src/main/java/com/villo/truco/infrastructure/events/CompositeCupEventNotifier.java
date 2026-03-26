package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.CupEventContext;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class CompositeCupEventNotifier
    extends CompositeEventDispatcher<CupEventContext>
    implements CupEventNotifier {

    public CompositeCupEventNotifier(final List<CupDomainEventHandler<?>> handlers) {

        super(handlers);
    }

    @Override
    public void publishDomainEvents(final CupId cupId, final List<PlayerId> participants,
        final List<DomainEventBase> events) {

        this.dispatchEvents(new CupEventContext(cupId, List.copyOf(participants)), events);
    }

}
