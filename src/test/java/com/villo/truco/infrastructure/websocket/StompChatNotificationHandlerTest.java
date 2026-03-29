package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.events.ChatEventNotification;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompChatNotificationHandler")
class StompChatNotificationHandlerTest {

  @Test
  @DisplayName("envia broadcast a todos los participantes")
  void broadcastsToAllParticipants() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompChatNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var chatId = ChatId.generate();
    final var recipients = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());

    handler.handle(
        new ChatEventNotification(chatId, recipients, "CHAT_CREATED", System.currentTimeMillis(),
            Map.of("parentType", "MATCH", "parentId", "parent-123")));

    verify(messaging, times(3)).convertAndSendToUser(any(), eq("/queue/chat"), any());
  }

}
