package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.ports.PlayerIdentity;
import com.villo.truco.application.ports.PlayerTokenProvider;
import java.util.Objects;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

public final class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final String IDENTITY_ATTR = "playerIdentity";
  private static final String TOKEN_HEADER = "Authorization";

  private final PlayerTokenProvider tokenProvider;

  public WebSocketAuthInterceptor(final PlayerTokenProvider tokenProvider) {

    this.tokenProvider = Objects.requireNonNull(tokenProvider);
  }

  @Override
  public Message<?> preSend(final Message<?> message, final MessageChannel channel) {

    final var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      return this.handleConnect(message, accessor);
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      return this.handleSubscribe(message, accessor);
    }

    return message;
  }

  private Message<?> handleConnect(final Message<?> message, final StompHeaderAccessor accessor) {

    final var authHeader = accessor.getFirstNativeHeader(TOKEN_HEADER);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new MessageDeliveryException("Missing or invalid Authorization header");
    }

    final var token = authHeader.substring(7);
    final var identity = this.tokenProvider.validateAccessToken(token);

    accessor.getSessionAttributes().put(IDENTITY_ATTR, identity);

    return message;
  }

  private Message<?> handleSubscribe(final Message<?> message, final StompHeaderAccessor accessor) {

    final var identity = (PlayerIdentity) accessor.getSessionAttributes().get(IDENTITY_ATTR);
    if (identity == null) {
      throw new MessageDeliveryException("Not authenticated");
    }

    final var destination = accessor.getDestination();
    if (destination != null && destination.startsWith("/topic/matches/")) {
      this.validateTopicAccess(destination, identity);
    }

    return message;
  }

  private void validateTopicAccess(final String destination, final PlayerIdentity identity) {

    // Topic format: /topic/matches/{matchId}/{playerId}
    final var parts = destination.split("/");
    // parts: ["", "topic", "matches", matchId, playerId]
    if (parts.length >= 5) {
      final var topicMatchId = parts[3];
      final var topicPlayerId = parts[4];

      final var matchIdMatches = identity.matchId().value().toString().equals(topicMatchId);
      final var playerIdMatches = identity.playerId().value().toString().equals(topicPlayerId);

      if (!matchIdMatches || !playerIdMatches) {
        throw new MessageDeliveryException("Not authorized to subscribe to this topic");
      }
    }
  }

}
