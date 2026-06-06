package com.villo.truco.infrastructure.websocket;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.ports.out.FriendOnlinePresencePort;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public final class WebSocketSessionPresenceTracker implements FriendOnlinePresencePort {

  private static final String IDENTITY_ATTR = "authenticatedPlayer";

  private final Map<PlayerId, ConcurrentSkipListSet<String>> sessionsByPlayer = new ConcurrentHashMap<>();
  private final Map<String, PlayerId> playerBySession = new ConcurrentHashMap<>();
  private final FriendPresenceAvailabilityNotifier presenceAvailabilityNotifier;

  public WebSocketSessionPresenceTracker(
      final FriendPresenceAvailabilityNotifier presenceAvailabilityNotifier) {

    this.presenceAvailabilityNotifier = Objects.requireNonNull(presenceAvailabilityNotifier);
  }

  private static String extractPlayerId(final StompHeaderAccessor accessor) {

    final var attrs = accessor.getSessionAttributes();
    if (attrs != null) {
      final var playerId = attrs.get(IDENTITY_ATTR);
      if (playerId instanceof String value && !value.isBlank()) {
        return value;
      }
    }

    final var user = accessor.getUser();
    if (user == null || Objects.requireNonNullElse(user.getName(), "").isBlank()) {
      return null;
    }
    return user.getName();
  }

  @Override
  public boolean isOnline(final PlayerId playerId) {

    final var sessions = this.sessionsByPlayer.get(playerId);
    return sessions != null && !sessions.isEmpty();
  }

  @EventListener
  public void onConnect(final SessionConnectEvent event) {

    final var accessor = StompHeaderAccessor.wrap(event.getMessage());
    final var sessionId = accessor.getSessionId();
    final var playerId = extractPlayerId(accessor);
    if (sessionId == null || playerId == null) {
      return;
    }

    final var parsedPlayerId = PlayerId.of(playerId);
    this.playerBySession.put(sessionId, parsedPlayerId);
    final var sessions = this.sessionsByPlayer.computeIfAbsent(parsedPlayerId,
        ignored -> new ConcurrentSkipListSet<>());
    final var becameOnline = sessions.isEmpty();
    sessions.add(sessionId);

    if (becameOnline) {
      this.presenceAvailabilityNotifier.notifyPresenceChanged(parsedPlayerId);
    }
  }

  @EventListener
  public void onDisconnect(final SessionDisconnectEvent event) {

    final var sessionId = event.getSessionId();
    if (sessionId == null) {
      return;
    }

    final var playerId = this.playerBySession.remove(sessionId);
    if (playerId == null) {
      return;
    }

    final var sessions = this.sessionsByPlayer.get(playerId);
    if (sessions == null) {
      return;
    }
    sessions.remove(sessionId);
    if (sessions.isEmpty()) {
      this.sessionsByPlayer.remove(playerId, sessions);
      this.presenceAvailabilityNotifier.notifyPresenceChanged(playerId);
    }
  }

}
