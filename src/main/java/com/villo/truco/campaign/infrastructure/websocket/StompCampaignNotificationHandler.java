package com.villo.truco.campaign.infrastructure.websocket;

import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.campaign.application.events.CampaignEventNotification;
import com.villo.truco.campaign.infrastructure.websocket.dto.CampaignWsEvent;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompCampaignNotificationHandler implements
    ApplicationEventHandler<CampaignEventNotification> {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;

  public StompCampaignNotificationHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.eventNotifierHealthRegistry = Objects.requireNonNull(eventNotifierHealthRegistry);
  }

  @Override
  public Class<CampaignEventNotification> eventType() {

    return CampaignEventNotification.class;
  }

  @Override
  public void handle(final CampaignEventNotification notification) {

    final var wsEvent = new CampaignWsEvent(notification.eventType(), notification.timestamp(),
        notification.payload());
    for (final var recipient : notification.recipients()) {
      try {
        this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(recipient),
            "/queue/campaign", wsEvent);
        this.eventNotifierHealthRegistry.recordSuccess();
      } catch (final RuntimeException ex) {
        this.eventNotifierHealthRegistry.recordFailure(ex);
        throw ex;
      }
    }
  }

}
