package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.MatchDerivedWsEvent;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEventBase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompMatchNotificationHandler implements
    ApplicationEventHandler<MatchEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompMatchNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompMatchNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<MatchEventNotification> eventType() {

    return MatchEventNotification.class;
  }

  @Override
  public void handle(final MatchEventNotification notification) {

    LOGGER.debug("Publishing match event matchId={} type={}", notification.matchId(),
        notification.eventType());
    final var destination =
        notification.stateVersion() == null ? "/queue/match-derived" : "/queue/match";
    final MatchWsEventBase wsEvent;
    if (notification.stateVersion() == null) {
      wsEvent = new MatchDerivedWsEvent(notification.matchId().value().toString(),
          notification.eventType(), notification.timestamp(), notification.payload());
    } else {
      wsEvent = new MatchWsEvent(notification.matchId().value().toString(),
          notification.eventType(), notification.timestamp(), notification.payload(),
          notification.stateVersion());
    }
    for (final var recipient : notification.recipients()) {
      sendEvent(recipient, destination, wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final String destination,
      final MatchWsEventBase message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending match WS event to user={} destination={}", userName, destination);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, destination, message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
