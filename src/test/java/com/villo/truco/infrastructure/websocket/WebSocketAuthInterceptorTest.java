package com.villo.truco.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class WebSocketAuthInterceptorTest {

  @Test
  void shouldRejectConnectWithoutAuthorizationHeader() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);
    final var message = this.connectMessage(null);

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldRejectConnectWithInvalidToken() {

    final var decoder = mock(JwtDecoder.class);
    when(decoder.decode("invalid")).thenThrow(new JwtException("bad token"));
    final var interceptor = new WebSocketAuthInterceptor(decoder);
    final var message = this.connectMessage("Bearer invalid");

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAuthenticateConnectWithValidToken() {

    final var decoder = mock(JwtDecoder.class);
    final var matchId = UUID.randomUUID().toString();
    final var playerId = UUID.randomUUID().toString();

    when(decoder.decode("ok-token")).thenReturn(this.jwt(matchId, playerId));

    final var interceptor = new WebSocketAuthInterceptor(decoder);
    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionAttributes(new java.util.HashMap<>());
    accessor.setNativeHeader("Authorization", "Bearer ok-token");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    final var output = interceptor.preSend(message, null);
    final var outputAccessor = MessageHeaderAccessor.getAccessor(output, StompHeaderAccessor.class);

    assertNotNull(outputAccessor.getUser());
    assertEquals(playerId, outputAccessor.getSessionAttributes().get("authenticatedPlayer"));
  }

  @Test
  void shouldRejectSubscribeWithoutAuthenticatedSession() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionAttributes(new java.util.HashMap<>());
    accessor.setDestination("/user/queue/match");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldRejectSubscribeToUnauthorizedDestination() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/topic/global");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToUserQueueWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/user/queue/match");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToChatQueueWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/user/queue/chat");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToLeagueQueueWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/user/queue/league");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToCupQueueWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/user/queue/cup");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToPublicMatchLobbyTopicWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/topic/public-match-lobby");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToPublicCupLobbyTopicWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/topic/public-cup-lobby");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeToPublicLeagueLobbyTopicWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/topic/public-league-lobby");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  @Test
  void shouldRejectSubscribeToOldEventsQueueWhenAuthenticated() {

    final var decoder = mock(JwtDecoder.class);
    final var interceptor = new WebSocketAuthInterceptor(decoder);

    final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    final var attrs = new java.util.HashMap<String, Object>();
    attrs.put("authenticatedPlayer", UUID.randomUUID().toString());
    accessor.setSessionAttributes(attrs);
    accessor.setDestination("/user/queue/events");

    final Message<byte[]> message = MessageBuilderSupport.build(accessor);

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  private Message<byte[]> connectMessage(final String authorizationHeader) {

    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionAttributes(new java.util.HashMap<>());
    if (authorizationHeader != null) {
      accessor.setNativeHeader("Authorization", authorizationHeader);
    }
    return MessageBuilderSupport.build(accessor);
  }

  private Jwt jwt(final String matchId, final String subject) {

    return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(300),
        Map.of("alg", "HS256"),
        Map.of("sub", subject, "matchId", matchId, "aud", List.of("test-audience"), "iss",
            "test-issuer"));
  }

  private static final class MessageBuilderSupport {

    private MessageBuilderSupport() {

    }

    private static Message<byte[]> build(final StompHeaderAccessor accessor) {

      accessor.setLeaveMutable(true);
      return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

  }

}
