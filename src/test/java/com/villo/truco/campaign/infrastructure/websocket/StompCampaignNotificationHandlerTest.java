package com.villo.truco.campaign.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.campaign.application.events.CampaignEventNotification;
import com.villo.truco.campaign.infrastructure.websocket.dto.CampaignWsEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompCampaignNotificationHandler")
class StompCampaignNotificationHandlerTest {

  @Test
  @DisplayName("publica CAMPAIGN_MATCH_POINTS en /queue/campaign")
  void publishesMatchPointsToCampaignQueue() {

    final var template = mock(SimpMessagingTemplate.class);
    final var handler = new StompCampaignNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));
    final var playerId = PlayerId.of("11111111-1111-1111-1111-111111111111");
    final var payload = Map.<String, Object>of("won", true, "pointsAwarded", 300, "totalPoints",
        300, "previousPosition", 4, "newPosition", 1);

    handler.handle(
        new CampaignEventNotification(List.of(playerId), "CAMPAIGN_MATCH_POINTS", 2L, payload));

    verify(template).convertAndSendToUser("11111111-1111-1111-1111-111111111111", "/queue/campaign",
        new CampaignWsEvent("CAMPAIGN_MATCH_POINTS", 2L, payload));
  }

}
