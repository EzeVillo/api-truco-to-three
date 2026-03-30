package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.LeagueEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.LeagueWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompLeagueNotificationHandler implements
    ApplicationEventHandler<LeagueEventNotification> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StompLeagueNotificationHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompLeagueNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<LeagueEventNotification> eventType() {

    return LeagueEventNotification.class;
  }

  @Override
  public void handle(final LeagueEventNotification notification) {

    LOGGER.debug("Publishing league event leagueId={} type={}", notification.leagueId(),
        notification.eventType());
    final var wsEvent = new LeagueWsEvent(notification.leagueId().value().toString(),
        notification.eventType(), notification.timestamp(), notification.payload());
    for (final var recipient : notification.recipients()) {
      sendEvent(recipient, wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending league WS event to user={}", userName);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, "/queue/league", message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
