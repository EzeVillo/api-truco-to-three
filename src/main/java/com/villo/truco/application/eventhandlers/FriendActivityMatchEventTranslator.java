package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.events.FriendActivityNotification;
import com.villo.truco.social.application.events.FriendAvailabilityNotification;
import com.villo.truco.social.application.services.FriendActivityResolver;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FriendActivityMatchEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  public static final String CHANGED_EVENT_TYPE = "FRIEND_ACTIVITY_CHANGED";
  public static final String AVAILABILITY_CHANGED_EVENT_TYPE = "FRIEND_AVAILABILITY_CHANGED";

  private final FriendActivityResolver friendActivityResolver;
  private final FriendAvailabilityResolver friendAvailabilityResolver;
  private final ApplicationEventPublisher applicationEventPublisher;

  public FriendActivityMatchEventTranslator(final FriendActivityResolver friendActivityResolver,
      final FriendAvailabilityResolver friendAvailabilityResolver,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.friendActivityResolver = Objects.requireNonNull(friendActivityResolver);
    this.friendAvailabilityResolver = Objects.requireNonNull(friendAvailabilityResolver);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  private static boolean startsSpectatableActivity(final MatchDomainEvent event) {

    return event instanceof GameStartedEvent gameStarted && gameStarted.getGameNumber() == 1;
  }

  private static boolean stopsSpectatableActivity(final MatchDomainEvent event) {

    return event instanceof MatchFinishedEvent || event instanceof MatchCancelledEvent
        || event instanceof MatchAbandonedEvent || event instanceof MatchForfeitedEvent;
  }

  private static Map<String, Object> payload(final FriendActivityDTO activity) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friendUsername", activity.friendUsername());
    payload.put("spectatableMatch", activity.spectatableMatch());
    return payload;
  }

  private static Map<String, Object> availabilityPayload(final FriendAvailabilityDTO availability) {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friendUsername", availability.friendUsername());
    payload.put("online", availability.online());
    payload.put("availability", availability.availability());
    payload.put("busyReason", availability.busyReason());
    payload.put("spectatableMatch", availability.spectatableMatch());
    return payload;
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final boolean spectatable;
    if (startsSpectatableActivity(event)) {
      spectatable = true;
    } else if (stopsSpectatableActivity(event)) {
      spectatable = false;
    } else {
      return;
    }

    this.publishChanges(event, event.getPlayerOne(), spectatable);
    if (event.getPlayerTwo() != null) {
      this.publishChanges(event, event.getPlayerTwo(), spectatable);
    }
  }

  private void publishChanges(final MatchDomainEvent event, final PlayerId activePlayer,
      final boolean spectatable) {

    final var changes = this.friendActivityResolver.resolveMatchActivityChangesByRecipient(
        event.getMatchId(), event.getPlayerOne(), event.getPlayerTwo(), activePlayer, spectatable);
    for (final var entry : changes.entrySet()) {
      this.applicationEventPublisher.publish(
          new FriendActivityNotification(List.of(entry.getKey()), CHANGED_EVENT_TYPE,
              event.getTimestamp(), payload(entry.getValue())));
    }

    final var availabilityChanges = this.friendAvailabilityResolver.resolveAvailabilityChangesByRecipient(
        event.getPlayerOne(), event.getPlayerTwo(), activePlayer);
    for (final var entry : availabilityChanges.entrySet()) {
      this.applicationEventPublisher.publish(
          new FriendAvailabilityNotification(List.of(entry.getKey()),
              AVAILABILITY_CHANGED_EVENT_TYPE, event.getTimestamp(),
              availabilityPayload(entry.getValue())));
    }
  }

}
