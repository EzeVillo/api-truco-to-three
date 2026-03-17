package com.villo.truco.application.ports.out;

import com.villo.truco.domain.shared.DomainEventBase;

public interface MatchDomainEventHandler<E extends DomainEventBase> {

  Class<E> eventType();

  void handle(E event, MatchEventContext context);

}
