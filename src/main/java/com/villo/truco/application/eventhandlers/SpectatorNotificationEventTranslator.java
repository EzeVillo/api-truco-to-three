package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.SpectatorMatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.SeatTargetedEvent;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import java.util.Objects;

public final class SpectatorNotificationEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final SpectatorshipRepository spectatorshipRepository;
  private final MatchEventMapper mapper;
  private final ApplicationEventPublisher publisher;

  public SpectatorNotificationEventTranslator(final SpectatorshipRepository spectatorshipRepository,
      final MatchEventMapper mapper, final ApplicationEventPublisher publisher) {

    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.mapper = Objects.requireNonNull(mapper);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;

    if (inner instanceof SeatTargetedEvent) {
      return;
    }

    final var spectatorIds = this.spectatorshipRepository.findActiveSpectatorIdsByMatchId(
        event.getMatchId());
    if (spectatorIds.isEmpty()) {
      return;
    }

    final var payload = this.mapper.map(inner);
    this.publisher.publish(
        new SpectatorMatchEventNotification(event.getMatchId(), spectatorIds, inner.getEventType(),
            inner.getTimestamp(), payload));
  }

}
