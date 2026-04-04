package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.PublicMatchLobbyNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.PublicLobbyWsEvent;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompPublicMatchLobbyNotificationHandler implements
    ApplicationEventHandler<PublicMatchLobbyNotification> {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompPublicMatchLobbyNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<PublicMatchLobbyNotification> eventType() {

    return PublicMatchLobbyNotification.class;
  }

  @Override
  public void handle(final PublicMatchLobbyNotification notification) {

    sendEvent(new PublicLobbyWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload()), "/topic/public-match-lobby");
  }

  private void sendEvent(final Object message, final String destination) {

    try {
      this.messagingTemplate.convertAndSend(destination, message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
