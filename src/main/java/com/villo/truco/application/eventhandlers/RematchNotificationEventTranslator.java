package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.domain.model.rematch.events.RematchPlayerWantsRematchEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RematchNotificationEventTranslator implements
    RematchSessionDomainEventHandler<RematchSessionDomainEvent> {

  private final ApplicationEventPublisher publisher;
  private final PublicActorResolver publicActorResolver;

  public RematchNotificationEventTranslator(final ApplicationEventPublisher publisher,
      final PublicActorResolver publicActorResolver) {

    this.publisher = Objects.requireNonNull(publisher);
    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
  }

  @Override
  public Class<RematchSessionDomainEvent> eventType() {

    return RematchSessionDomainEvent.class;
  }

  @Override
  public void handle(final RematchSessionDomainEvent event) {

    if (event instanceof RematchSessionOpenedEvent opened) {
      handleOpened(opened);
    } else if (event instanceof RematchPlayerWantsRematchEvent wants) {
      handleWantsRematch(wants);
    } else if (event instanceof RematchSessionConfirmedEvent confirmed) {
      handleConfirmed(confirmed);
    } else if (event instanceof RematchSessionClosedByLeaveEvent closed) {
      handleClosedByLeave(closed);
    } else if (event instanceof RematchSessionExpiredEvent expired) {
      handleExpired(expired);
    }
  }

  private void handleOpened(final RematchSessionOpenedEvent event) {

    final List<PlayerId> recipients = new ArrayList<>();
    if (!event.isPlayerOneIsBot()) {
      recipients.add(event.getPlayerOneId());
    }
    if (!event.isPlayerTwoIsBot()) {
      recipients.add(event.getPlayerTwoId());
    }

    final Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", event.getRematchSessionId().value().toString());
    payload.put("originMatchId", event.getOriginMatchId().value().toString());
    payload.put("expiresAt", event.getExpiresAt().toEpochMilli());

    publish(event, recipients, payload);
  }

  private void handleWantsRematch(final RematchPlayerWantsRematchEvent event) {

    final Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", event.getRematchSessionId().value().toString());
    payload.put("originMatchId", event.getOriginMatchId().value().toString());
    payload.put("actor", publicActorResolver.resolve(event.getActorId()));

    publish(event, List.of(event.getOtherPlayerId()), payload);
  }

  private void handleConfirmed(final RematchSessionConfirmedEvent event) {

    final Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", event.getRematchSessionId().value().toString());
    payload.put("originMatchId", event.getOriginMatchId().value().toString());
    payload.put("newMatchId", event.getNewMatchId().value().toString());
    payload.put("newPlayerOne", publicActorResolver.resolve(event.getNewPlayerOneId()));
    payload.put("newPlayerTwo", publicActorResolver.resolve(event.getNewPlayerTwoId()));

    publish(event, List.of(event.getNewPlayerOneId(), event.getNewPlayerTwoId()), payload);
  }

  private void handleClosedByLeave(final RematchSessionClosedByLeaveEvent event) {

    final Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", event.getRematchSessionId().value().toString());
    payload.put("originMatchId", event.getOriginMatchId().value().toString());
    payload.put("actor", publicActorResolver.resolve(event.getActorId()));

    publish(event, List.of(event.getOtherPlayerId()), payload);
  }

  private void handleExpired(final RematchSessionExpiredEvent event) {

    final Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", event.getRematchSessionId().value().toString());
    payload.put("originMatchId", event.getOriginMatchId().value().toString());

    publish(event, List.of(event.getPlayerOneId(), event.getPlayerTwoId()), payload);
  }

  private void publish(final RematchSessionDomainEvent event, final List<PlayerId> recipients,
      final Map<String, Object> payload) {

    publisher.publish(
        new MatchEventNotification(event.getOriginMatchId(), recipients, event.getEventType(),
            event.getTimestamp(), payload));
  }

}
