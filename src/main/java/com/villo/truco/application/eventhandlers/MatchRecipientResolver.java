package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.SeatTargetedEvent;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class MatchRecipientResolver {

  public List<PlayerId> resolve(final MatchDomainEvent outerEvent,
      final DomainEventBase innerEvent) {

    if (innerEvent instanceof SeatTargetedEvent targeted) {
      return List.of(outerEvent.resolvePlayer(targeted.getTargetSeat()));
    }
    return Stream.of(outerEvent.getPlayerOne(), outerEvent.getPlayerTwo()).filter(Objects::nonNull)
        .toList();
  }

}
