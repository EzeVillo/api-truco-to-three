package com.villo.truco.infrastructure.websocket;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public final class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final String IDENTITY_ATTR = "authenticatedPlayer";
  private static final String TOKEN_HEADER = "Authorization";
  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

  private final JwtDecoder jwtDecoder;

  public WebSocketAuthInterceptor(final JwtDecoder jwtDecoder) {

    this.jwtDecoder = Objects.requireNonNull(jwtDecoder);
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
      LOGGER.warn("WS connect rejected: missing/invalid Authorization header");
      throw new MessageDeliveryException("Missing or invalid Authorization header");
    }

    final var token = authHeader.substring(7);
    final var jwt = this.decode(token);
    final var matchId = jwt.getClaimAsString("matchId");
    final var playerId = jwt.getSubject();

    if (matchId == null || playerId == null) {
      LOGGER.warn("WS connect rejected: missing claims in JWT");
      throw new MessageDeliveryException("Invalid authentication token claims");
    }

    accessor.getSessionAttributes().put(IDENTITY_ATTR, playerId);
    final var userName = WebSocketUserNaming.userName(new MatchId(UUID.fromString(matchId)),
        new PlayerId(UUID.fromString(playerId)));
    accessor.setUser(() -> userName);
    LOGGER.info("WS client authenticated: playerId={}, matchId={}", playerId, matchId);

    return message;
  }

  private Message<?> handleSubscribe(final Message<?> message, final StompHeaderAccessor accessor) {

    final var authenticatedPlayer = accessor.getSessionAttributes().get(IDENTITY_ATTR);
    if (authenticatedPlayer == null) {
      LOGGER.warn("WS subscribe rejected: unauthenticated session");
      throw new MessageDeliveryException("Not authenticated");
    }

    final var destination = accessor.getDestination();
    if (destination != null) {
      this.validateTopicAccess(destination);
      LOGGER.debug("WS subscribe authorized: destination={}", destination);
    }

    return message;
  }

  private void validateTopicAccess(final String destination) {

    // Topic format:
    //   /user/queue/events
    if ("/user/queue/events".equals(destination)) {
      return;
    }

    throw new MessageDeliveryException("Not authorized to subscribe to this topic");
  }

  private Jwt decode(final String token) {

    try {
      return this.jwtDecoder.decode(token);
    } catch (final JwtException ex) {
      LOGGER.warn("WS connect rejected: invalid JWT");
      throw new MessageDeliveryException("Invalid authentication token");
    }
  }

}
