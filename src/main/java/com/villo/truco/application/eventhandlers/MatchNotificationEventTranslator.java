package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.HandDealtEvent;
import com.villo.truco.domain.model.match.events.MatchDerivedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class MatchNotificationEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final MatchEventMapper mapper;
  private final MatchRecipientResolver recipientResolver;
  private final ApplicationEventPublisher publisher;

  public MatchNotificationEventTranslator(final MatchEventMapper mapper,
      final MatchRecipientResolver recipientResolver, final ApplicationEventPublisher publisher) {

    this.mapper = Objects.requireNonNull(mapper);
    this.recipientResolver = Objects.requireNonNull(recipientResolver);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (event instanceof PublicMatchLobbyOpenedEvent) {
      return;
    }

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;

    if (inner instanceof HandDealtEvent handDealt) {
      this.publishHandDealt(event, handDealt);
      return;
    }

    final var payload = this.mapper.map(inner);
    final var recipients = this.recipientResolver.resolve(event, inner);
    final Long stateVersion = inner instanceof MatchDerivedEvent ? null : event.getStateVersion();
    this.publisher.publish(
        new MatchEventNotification(event.getMatchId(), recipients, inner.getEventType(),
            inner.getTimestamp(), payload, stateVersion));
  }

  private void publishHandDealt(final MatchDomainEvent outerEvent, final HandDealtEvent handDealt) {

    for (final var seat : List.of(PlayerSeat.PLAYER_ONE, PlayerSeat.PLAYER_TWO)) {
      final var recipient = outerEvent.resolvePlayer(seat);
      final var payload = new LinkedHashMap<String, Object>();
      payload.put("seat", seat.name());
      payload.put("cards",
          handDealt.getCardsForSeat(seat).stream().map(MatchEventMapper::mapCard).toList());
      this.publisher.publish(new MatchEventNotification(outerEvent.getMatchId(), List.of(recipient),
          handDealt.getEventType(), handDealt.getTimestamp(), payload,
          outerEvent.getStateVersion()));
    }
  }

}
