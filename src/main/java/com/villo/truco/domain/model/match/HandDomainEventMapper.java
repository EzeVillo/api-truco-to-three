package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.HandChangedEvent;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.ArrayList;
import java.util.List;

final class HandDomainEventMapper {

  private HandDomainEventMapper() {

  }

  static List<PlayerHandUpdatedEvent> toPlayerHandUpdatedEvents(
      final List<DomainEventBase> handDomainEvents, final PlayerSeat seat) {

    final var translatedEvents = new ArrayList<PlayerHandUpdatedEvent>();

    for (final var event : handDomainEvents) {
      if (event instanceof HandChangedEvent changed) {
        translatedEvents.add(new PlayerHandUpdatedEvent(seat, changed.getRemainingCards()));
      }
    }

    return translatedEvents;
  }

}