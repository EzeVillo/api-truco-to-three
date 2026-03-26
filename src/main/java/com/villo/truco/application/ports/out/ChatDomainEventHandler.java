package com.villo.truco.application.ports.out;

import com.villo.truco.domain.shared.DomainEventBase;

public interface ChatDomainEventHandler<E extends DomainEventBase>
    extends DomainEventHandler<E, ChatEventContext> {

}
