package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public final class InMemoryQuickMatchQueueAdapter implements QuickMatchQueuePort {

  private final ConcurrentHashMap<GamesToPlay, ConcurrentLinkedDeque<QuickMatchTicket>> queues = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<PlayerId, QuickMatchTicket> byPlayer = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PlayerId> bySessionId = new ConcurrentHashMap<>();

  @Override
  public void enqueue(final QuickMatchTicket ticket) {

    final var deque = queues.computeIfAbsent(ticket.gamesToPlay(),
        k -> new ConcurrentLinkedDeque<>());
    synchronized (deque) {
      deque.addLast(ticket);
      byPlayer.put(ticket.playerId(), ticket);
      if (ticket.webSocketSessionId() != null) {
        bySessionId.put(ticket.webSocketSessionId(), ticket.playerId());
      }
    }
  }

  @Override
  public Optional<QuickMatchTicket> tryDequeue(final PlayerId playerId) {

    final var ticket = byPlayer.remove(playerId);
    if (ticket == null) {
      return Optional.empty();
    }
    final var deque = queues.get(ticket.gamesToPlay());
    if (deque != null) {
      synchronized (deque) {
        deque.remove(ticket);
        if (ticket.webSocketSessionId() != null) {
          bySessionId.remove(ticket.webSocketSessionId());
        }
      }
    }
    return Optional.of(ticket);
  }

  @Override
  public Optional<QuickMatchTicket> tryMatchOpponent(final PlayerId enqueuingPlayer,
      final GamesToPlay gamesToPlay) {

    final var deque = queues.get(gamesToPlay);
    if (deque == null) {
      return Optional.empty();
    }
    synchronized (deque) {
      for (final var ticket : deque) {
        if (!ticket.playerId().equals(enqueuingPlayer)) {
          deque.remove(ticket);
          byPlayer.remove(ticket.playerId());
          if (ticket.webSocketSessionId() != null) {
            bySessionId.remove(ticket.webSocketSessionId());
          }
          return Optional.of(ticket);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean isPlayerQueued(final PlayerId playerId) {

    return byPlayer.containsKey(playerId);
  }

  @Override
  public Optional<QuickMatchTicket> findByPlayer(final PlayerId playerId) {

    return Optional.ofNullable(byPlayer.get(playerId));
  }

  @Override
  public Optional<QuickMatchTicket> tryDequeueBySessionId(final String sessionId) {

    final var playerId = bySessionId.get(sessionId);
    if (playerId == null) {
      return Optional.empty();
    }
    return tryDequeue(playerId);
  }

}
