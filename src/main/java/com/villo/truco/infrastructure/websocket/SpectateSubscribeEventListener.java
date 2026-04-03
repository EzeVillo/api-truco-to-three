package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
final class SpectateSubscribeEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      SpectateSubscribeEventListener.class);
  private static final String SPECTATE_DESTINATION = "/user/queue/match-spectate";
  private static final String MATCH_ID_HEADER = "matchId";
  private static final String IDENTITY_ATTR = "authenticatedPlayer";

  private final SpectateMatchUseCase spectateMatchUseCase;
  private final SimpMessagingTemplate messagingTemplate;
  private final SpectateSessionRegistry sessionRegistry;

  SpectateSubscribeEventListener(final SpectateMatchUseCase spectateMatchUseCase,
      final SimpMessagingTemplate messagingTemplate,
      final SpectateSessionRegistry sessionRegistry) {

    this.spectateMatchUseCase = Objects.requireNonNull(spectateMatchUseCase);
    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry);
  }

  private static String extractAuthenticatedPlayer(final StompHeaderAccessor accessor) {

    final var attrs = accessor.getSessionAttributes();
    if (attrs == null) {
      return null;
    }

    return (String) attrs.get(IDENTITY_ATTR);
  }

  @EventListener
  public void onSubscribe(final SessionSubscribeEvent event) {

    final var accessor = StompHeaderAccessor.wrap(event.getMessage());
    if (!SPECTATE_DESTINATION.equals(accessor.getDestination())) {
      return;
    }

    final var playerId = extractAuthenticatedPlayer(accessor);
    final var matchId = accessor.getFirstNativeHeader(MATCH_ID_HEADER);

    if (playerId == null || matchId == null) {
      LOGGER.warn("Spectate subscribe rejected: missing playerId or matchId header");
      return;
    }

    try {
      final var state = this.spectateMatchUseCase.handle(
          new SpectateMatchCommand(matchId, playerId));

      this.sessionRegistry.register(accessor.getSessionId(), accessor.getSubscriptionId(), playerId,
          matchId);

      sendInitialState(playerId, state);
      LOGGER.info("Spectator joined: playerId={}, matchId={}", playerId, matchId);
    } catch (final RuntimeException ex) {
      LOGGER.warn("Spectate subscribe failed: playerId={}, matchId={}, error={}", playerId, matchId,
          ex.getMessage());
      sendError(playerId, ex.getMessage());
    }
  }

  private void sendInitialState(final String playerId, final SpectatorMatchStateDTO state) {

    final var userName = WebSocketUserNaming.userName(playerId);
    final var wsEvent = new MatchWsEvent(state.matchId(), "SPECTATE_STATE",
        System.currentTimeMillis(), Map.of("matchState", state));
    this.messagingTemplate.convertAndSendToUser(userName, "/queue/match-spectate", wsEvent);
  }

  private void sendError(final String playerId, final String errorMessage) {

    final var userName = WebSocketUserNaming.userName(playerId);
    final var wsEvent = new MatchWsEvent(null, "SPECTATE_ERROR", System.currentTimeMillis(),
        Map.of("error", errorMessage));
    this.messagingTemplate.convertAndSendToUser(userName, "/queue/match-spectate", wsEvent);
  }

}
