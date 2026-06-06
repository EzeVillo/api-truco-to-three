package com.villo.truco.social.infrastructure.websocket;

import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import com.villo.truco.social.application.events.FriendActivityNotification;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompFriendActivityNotificationHandler implements
    ApplicationEventHandler<FriendActivityNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StompFriendActivityNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompFriendActivityNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<FriendActivityNotification> eventType() {

    return FriendActivityNotification.class;
  }

  @Override
  public void handle(final FriendActivityNotification notification) {

    final var wsEvent = new SocialWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload());
    for (final var recipient : notification.recipients()) {
      try {
        this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(recipient),
            "/queue/social", wsEvent);
        this.healthRegistry.recordSuccess();
      } catch (final RuntimeException ex) {
        LOGGER.error("Failed to publish friend activity event to {}", recipient, ex);
        this.healthRegistry.recordFailure(ex);
        throw ex;
      }
    }
  }

}
