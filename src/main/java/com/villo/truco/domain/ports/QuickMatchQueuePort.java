package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface QuickMatchQueuePort {

  void enqueue(QuickMatchTicket ticket);

  Optional<QuickMatchTicket> tryDequeue(PlayerId playerId);

  Optional<QuickMatchTicket> tryMatchOpponent(PlayerId enqueuingPlayer, GamesToPlay gamesToPlay);

  boolean isPlayerQueued(PlayerId playerId);

  Optional<QuickMatchTicket> findByPlayer(PlayerId playerId);

  Optional<QuickMatchTicket> tryDequeueBySessionId(String sessionId);

}
