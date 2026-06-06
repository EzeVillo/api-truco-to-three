package com.villo.truco.social.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.social.application.events.FriendActivityNotification;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompFriendActivityNotificationHandler")
class StompFriendActivityNotificationHandlerTest {

  @Test
  @DisplayName("publica actividad de amigo en la cola social del usuario")
  void sendsFriendActivityToSocialQueue() {

    final var template = mock(SimpMessagingTemplate.class);
    final var handler = new StompFriendActivityNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));
    final var recipient = PlayerId.of("11111111-1111-1111-1111-111111111111");
    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friendUsername", "martina");
    payload.put("spectatableMatch", null);

    handler.handle(
        new FriendActivityNotification(List.of(recipient), "FRIEND_ACTIVITY_CHANGED", 1L, payload));

    verify(template).convertAndSendToUser("11111111-1111-1111-1111-111111111111", "/queue/social",
        new SocialWsEvent("FRIEND_ACTIVITY_CHANGED", 1L, payload));
  }

}
