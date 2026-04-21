package com.villo.truco.profile.infrastructure.websocket;

import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import com.villo.truco.profile.application.events.ProfileEventNotification;
import com.villo.truco.profile.infrastructure.websocket.dto.ProfileWsEvent;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompProfileNotificationHandler implements
    ApplicationEventHandler<ProfileEventNotification> {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;

  public StompProfileNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.eventNotifierHealthRegistry = Objects.requireNonNull(eventNotifierHealthRegistry);
  }

  @Override
  public Class<ProfileEventNotification> eventType() {

    return ProfileEventNotification.class;
  }

  @Override
  public void handle(final ProfileEventNotification notification) {

    final var wsEvent = new ProfileWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload());
    for (final var recipient : notification.recipients()) {
      try {
        this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(recipient),
            "/queue/profile", wsEvent);
        this.eventNotifierHealthRegistry.recordSuccess();
      } catch (final RuntimeException ex) {
        this.eventNotifierHealthRegistry.recordFailure(ex);
        throw ex;
      }
    }
  }
}
