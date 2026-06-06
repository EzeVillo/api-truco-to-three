package com.villo.truco.social.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import com.villo.truco.social.application.dto.FriendActivityStateDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStateDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.ports.in.GetFriendActivityUseCase;
import com.villo.truco.social.application.ports.in.GetFriendAvailabilityUseCase;
import com.villo.truco.social.application.queries.GetFriendActivityQuery;
import com.villo.truco.social.application.queries.GetFriendAvailabilityQuery;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@DisplayName("SocialSubscribeEventListener")
class SocialSubscribeEventListenerTest {

  private static Message<byte[]> subscribeMessage(final String playerId) {

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var sessionAttributes = new java.util.HashMap<String, Object>();
    sessionAttributes.put("authenticatedPlayer", playerId);
    accessor.setSessionAttributes(sessionAttributes);
    accessor.setDestination("/user/queue/social");
    accessor.setSessionId("session-1");
    accessor.setSubscriptionId("sub-1");
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("envia FRIEND_ACTIVITY_STATE al suscribirse a social")
  void sendsFriendActivityStateOnSocialSubscribe() {

    final var useCase = mock(GetFriendActivityUseCase.class);
    final var availabilityUseCase = mock(GetFriendAvailabilityUseCase.class);
    final var messaging = mock(SimpMessagingTemplate.class);
    final var listener = new SocialSubscribeEventListener(useCase, availabilityUseCase, messaging);
    final var playerId = "11111111-1111-1111-1111-111111111111";
    when(useCase.handle(new GetFriendActivityQuery(PlayerId.of(playerId)))).thenReturn(
        new FriendActivityStateDTO(List.of(new FriendActivityDTO("martina", null))));
    when(availabilityUseCase.handle(
        new GetFriendAvailabilityQuery(PlayerId.of(playerId)))).thenReturn(
        new FriendAvailabilityStateDTO(List.of(
            new FriendAvailabilityDTO("martina", true, FriendAvailabilityStatus.AVAILABLE, null,
                null))));

    listener.onSubscribe(new SessionSubscribeEvent(this, subscribeMessage(playerId)));

    final var eventCaptor = ArgumentCaptor.forClass(SocialWsEvent.class);
    verify(messaging, Mockito.times(2)).convertAndSendToUser(eq(playerId), eq("/queue/social"),
        eventCaptor.capture());
    assertThat(eventCaptor.getAllValues()).extracting(SocialWsEvent::eventType)
        .containsExactly("FRIEND_ACTIVITY_STATE", "FRIEND_AVAILABILITY_STATE");
    assertThat(eventCaptor.getAllValues()).allSatisfy(
        event -> assertThat(event.payload()).containsKey("friends"));
  }

}
