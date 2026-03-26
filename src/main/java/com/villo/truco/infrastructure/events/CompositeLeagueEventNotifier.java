package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class CompositeLeagueEventNotifier
    extends CompositeEventDispatcher<LeagueEventContext>
    implements LeagueEventNotifier {

    public CompositeLeagueEventNotifier(final List<LeagueDomainEventHandler<?>> handlers) {

        super(handlers);
    }

    @Override
    public void publishDomainEvents(final LeagueId leagueId, final List<PlayerId> participants,
        final List<DomainEventBase> events) {

        this.dispatchEvents(new LeagueEventContext(leagueId, List.copyOf(participants)), events);
    }

}
