package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.SpectatorMatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompSpectatorNotificationHandler implements
    ApplicationEventHandler<SpectatorMatchEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StompSpectatorNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompSpectatorNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<SpectatorMatchEventNotification> eventType() {

    return SpectatorMatchEventNotification.class;
  }

  @Override
  public void handle(final SpectatorMatchEventNotification notification) {

    LOGGER.debug("Publishing spectator match event matchId={} type={}", notification.matchId(),
        notification.eventType());
    final var wsEvent = new MatchWsEvent(notification.matchId().value().toString(),
        notification.eventType(), notification.timestamp(), notification.payload());
    for (final var spectatorId : notification.spectatorIds()) {
      this.sendEvent(spectatorId, wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending spectator WS event to user={}", userName);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, "/queue/match-spectate", message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
