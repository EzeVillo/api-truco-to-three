package com.villo.truco.social.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.social.application.events.SocialEventNotification;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompSocialNotificationHandler")
class StompSocialNotificationHandlerTest {

  @Test
  @DisplayName("publica en la cola social del usuario")
  void sendsSocialNotificationToUserQueue() {

    final var template = mock(SimpMessagingTemplate.class);
    final var payload = Map.<String, Object>of("requesterUsername", "juancho", "addresseeUsername",
        "martina");
    final var wsEvent = new SocialWsEvent("FRIEND_REQUEST_RECEIVED", 1L, payload);
    final var handler = new StompSocialNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));

    handler.handle(
        new SocialEventNotification(List.of(PlayerId.of("11111111-1111-1111-1111-111111111111")),
            "FRIEND_REQUEST_RECEIVED", 1L, payload));

    verify(template).convertAndSendToUser("11111111-1111-1111-1111-111111111111", "/queue/social",
        wsEvent);
  }

}
