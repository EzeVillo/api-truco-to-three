package com.villo.truco.profile.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.profile.application.events.ProfileEventNotification;
import com.villo.truco.profile.infrastructure.websocket.dto.ProfileWsEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompProfileNotificationHandler")
class StompProfileNotificationHandlerTest {

  @Test
  @DisplayName("publica ACHIEVEMENT_UNLOCKED en /queue/profile")
  void publishesAchievementUnlockedToProfileQueue() {

    final var template = mock(SimpMessagingTemplate.class);
    final var handler = new StompProfileNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));
    final var playerId = PlayerId.of("11111111-1111-1111-1111-111111111111");
    final var payload = Map.<String, Object>of("achievementCode", "WIN_RETRUCO_FROM_0_0_TO_3",
        "unlockedAt", 1L, "matchId", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "gameNumber", 1);

    handler.handle(new ProfileEventNotification(List.of(playerId), "ACHIEVEMENT_UNLOCKED", 2L,
        payload));

    verify(template).convertAndSendToUser("11111111-1111-1111-1111-111111111111", "/queue/profile",
        new ProfileWsEvent("ACHIEVEMENT_UNLOCKED", 2L, payload));
  }
}
