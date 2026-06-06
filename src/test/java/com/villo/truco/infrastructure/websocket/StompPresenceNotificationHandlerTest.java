package com.villo.truco.infrastructure.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.events.PresenceEventNotification;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.http.dto.response.UserPresenceResponse;
import com.villo.truco.infrastructure.websocket.dto.PresenceWsEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompPresenceNotificationHandler")
class StompPresenceNotificationHandlerTest {

  @Test
  @DisplayName("publica el snapshot en la cola de presencia del usuario destinatario")
  void sendsPresenceSnapshotToUserQueue() {

    final var template = mock(SimpMessagingTemplate.class);
    final var handler = new StompPresenceNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));
    final var recipient = PlayerId.of("11111111-1111-1111-1111-111111111111");
    final var snapshot = UserPresenceDTO.of(new ActiveMatchRefDTO("m-1", "IN_PROGRESS"), null, null,
        null, null);

    handler.handle(
        new PresenceEventNotification(recipient, PresenceEventNotification.EVENT_TYPE, 7L,
            snapshot));

    final var expected = new PresenceWsEvent(PresenceEventNotification.EVENT_TYPE, 7L,
        UserPresenceResponse.from(snapshot));
    verify(template).convertAndSendToUser("11111111-1111-1111-1111-111111111111", "/queue/presence",
        expected);
  }

}
