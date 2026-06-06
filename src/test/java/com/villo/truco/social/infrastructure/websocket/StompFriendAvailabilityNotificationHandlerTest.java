package com.villo.truco.social.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import com.villo.truco.social.application.events.FriendAvailabilityNotification;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompFriendAvailabilityNotificationHandler")
class StompFriendAvailabilityNotificationHandlerTest {

  @Test
  @DisplayName("envia el delta de disponibilidad a la cola social de cada destinatario")
  void sendsAvailabilityDeltaToEachRecipient() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var healthRegistry = mock(EventNotifierHealthRegistry.class);
    final var handler = new StompFriendAvailabilityNotificationHandler(messaging, healthRegistry);
    final var recipient = PlayerId.generate();

    handler.handle(
        new FriendAvailabilityNotification(List.of(recipient), "FRIEND_AVAILABILITY_CHANGED", 123L,
            Map.of("friendUsername", "martina")));

    verify(messaging).convertAndSendToUser(eq(WebSocketUserNaming.userName(recipient)),
        eq("/queue/social"), any(SocialWsEvent.class));
    verify(healthRegistry).recordSuccess();
  }

  @Test
  @DisplayName("sin destinatarios no envia ningun mensaje (amistad eliminada)")
  void sendsNothingWithoutRecipients() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var healthRegistry = mock(EventNotifierHealthRegistry.class);
    final var handler = new StompFriendAvailabilityNotificationHandler(messaging, healthRegistry);

    handler.handle(
        new FriendAvailabilityNotification(List.of(), "FRIEND_AVAILABILITY_CHANGED", 123L,
            Map.of("friendUsername", "martina")));

    verify(messaging, never()).convertAndSendToUser(any(String.class), any(String.class),
        any(Object.class));
  }

}
