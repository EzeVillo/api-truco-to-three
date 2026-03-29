package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.CupEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.CupWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompCupNotificationHandler implements
    ApplicationEventHandler<CupEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompCupNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompCupNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<CupEventNotification> eventType() {

    return CupEventNotification.class;
  }

  @Override
  public void handle(final CupEventNotification notification) {

    LOGGER.debug("Publishing cup event cupId={} type={}", notification.cupId(),
        notification.eventType());
    final var wsEvent = new CupWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload());
    for (final var recipient : notification.recipients()) {
      sendEvent(recipient, wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending cup WS event to user={}", userName);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, "/queue/events", message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
