package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.PresenceEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.http.dto.response.UserPresenceResponse;
import com.villo.truco.infrastructure.websocket.dto.PresenceWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompPresenceNotificationHandler implements
    ApplicationEventHandler<PresenceEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StompPresenceNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompPresenceNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<PresenceEventNotification> eventType() {

    return PresenceEventNotification.class;
  }

  @Override
  public void handle(final PresenceEventNotification notification) {

    final var wsEvent = new PresenceWsEvent(notification.eventType(), notification.timestamp(),
        UserPresenceResponse.from(notification.snapshot()));
    try {
      this.messagingTemplate.convertAndSendToUser(
          WebSocketUserNaming.userName(notification.recipient()), "/queue/presence", wsEvent);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      LOGGER.error("Failed to publish presence event to {}", notification.recipient(), ex);
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
