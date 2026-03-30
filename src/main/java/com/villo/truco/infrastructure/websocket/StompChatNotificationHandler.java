package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.ChatEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.ChatWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompChatNotificationHandler implements
    ApplicationEventHandler<ChatEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompChatNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompChatNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<ChatEventNotification> eventType() {

    return ChatEventNotification.class;
  }

  @Override
  public void handle(final ChatEventNotification notification) {

    LOGGER.debug("Publishing chat event chatId={} type={}", notification.chatId(),
        notification.eventType());
    final var wsEvent = new ChatWsEvent(notification.chatId().value().toString(),
        notification.eventType(), notification.timestamp(), notification.payload());
    for (final var recipient : notification.recipients()) {
      sendEvent(recipient, wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending chat WS event to user={}", userName);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, "/queue/chat", message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
