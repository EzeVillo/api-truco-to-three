package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@DisplayName("SpectateSubscribeEventListener")
class SpectateSubscribeEventListenerTest {

  private static Message<byte[]> subscribeMessage(final String playerId, final String matchId,
      final String sessionId, final String subscriptionId) {

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var sessionAttributes = new java.util.HashMap<String, Object>();
    sessionAttributes.put("authenticatedPlayer", playerId);
    accessor.setSessionAttributes(sessionAttributes);
    accessor.setDestination("/user/queue/match-spectate");
    accessor.setSessionId(sessionId);
    accessor.setSubscriptionId(subscriptionId);
    accessor.setNativeHeader("matchId", matchId);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("usa ids string de websocket y registra la suscripcion activa")
  void subscribesWithStringIdsAndRegistersSession() {

    final var useCase = mock(SpectateMatchUseCase.class);
    final var messaging = mock(SimpMessagingTemplate.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSubscribeEventListener(useCase, messaging, registry);
    final var playerId = java.util.UUID.randomUUID().toString();
    final var matchId = java.util.UUID.randomUUID().toString();

    when(useCase.handle(any())).thenReturn(
        new SpectatorMatchStateDTO(matchId, "IN_PROGRESS", 0, 0, null, null, 1));

    listener.onSubscribe(
        new SessionSubscribeEvent(this, subscribeMessage(playerId, matchId, "session-1", "sub-1")));

    final var commandCaptor = ArgumentCaptor.forClass(SpectateMatchCommand.class);
    verify(useCase).handle(commandCaptor.capture());
    verify(messaging).convertAndSendToUser(eq(playerId), eq("/queue/match-spectate"), any());

    assertThat(commandCaptor.getValue().matchId().value().toString()).isEqualTo(matchId);
    assertThat(commandCaptor.getValue().spectatorId().value().toString()).isEqualTo(playerId);
    assertThat(registry.removeSubscription("session-1", "sub-1")).isNotNull();
  }

}
