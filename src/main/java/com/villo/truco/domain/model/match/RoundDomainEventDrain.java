package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.EntityBase;
import java.util.List;

final class RoundDomainEventDrain {

  private RoundDomainEventDrain() {

  }

  static List<DomainEventBase> drainFrom(final EntityBase<?> entity) {

    final var events = entity.getDomainEvents();
    entity.clearDomainEvents();
    return events;
  }

}