package com.villo.truco.infrastructure.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
final class SpectateSessionRegistry {

  private final ConcurrentHashMap<String, SpectateSession> sessionsBySubscriptionKey = new ConcurrentHashMap<>();

  private static String toSubscriptionKey(final String sessionId, final String subscriptionId) {

    return Objects.requireNonNull(sessionId) + ":" + Objects.requireNonNull(subscriptionId);
  }

  void register(final String sessionId, final String subscriptionId, final String playerId,
      final String matchId) {

    this.sessionsBySubscriptionKey.put(toSubscriptionKey(sessionId, subscriptionId),
        new SpectateSession(playerId, matchId));
  }

  SpectateSession removeSubscription(final String sessionId, final String subscriptionId) {

    return this.sessionsBySubscriptionKey.remove(toSubscriptionKey(sessionId, subscriptionId));
  }

  List<SpectateSession> removeSession(final String sessionId) {

    final var removedSessions = new ArrayList<SpectateSession>();
    final var iterator = this.sessionsBySubscriptionKey.entrySet().iterator();

    while (iterator.hasNext()) {
      final var entry = iterator.next();
      if (entry.getKey().startsWith(sessionId + ":")) {
        removedSessions.add(entry.getValue());
        iterator.remove();
      }
    }

    return removedSessions;
  }

  record SpectateSession(String playerId, String matchId) {

  }

}
