package com.villo.truco.social.infrastructure.websocket;

import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import com.villo.truco.social.application.events.SocialEventNotification;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompSocialNotificationHandler implements
    ApplicationEventHandler<SocialEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StompSocialNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompSocialNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<SocialEventNotification> eventType() {

    return SocialEventNotification.class;
  }

  @Override
  public void handle(final SocialEventNotification notification) {

    final var wsEvent = new SocialWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload());
    for (final var recipient : notification.recipients()) {
      try {
        this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(recipient),
            "/queue/social", wsEvent);
        this.healthRegistry.recordSuccess();
      } catch (final RuntimeException ex) {
        LOGGER.error("Failed to publish social event to {}", recipient, ex);
        this.healthRegistry.recordFailure(ex);
        throw ex;
      }
    }
  }

}
