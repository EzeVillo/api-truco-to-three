package com.villo.truco.application.ports.out;

import com.villo.truco.domain.shared.DomainEventBase;

public interface DomainEventHandler<E extends DomainEventBase, C> {

    Class<E> eventType();

    void handle(E event, C context);

}
