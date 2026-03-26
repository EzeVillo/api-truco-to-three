package com.villo.truco.application.ports.out;

import com.villo.truco.domain.shared.DomainEventBase;

public interface LeagueDomainEventHandler<E extends DomainEventBase>
    extends DomainEventHandler<E, LeagueEventContext> {

}
