package com.villo.truco.testutil;

import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public final class NoOpQuickMatchQueuePort implements QuickMatchQueuePort {

  public static final NoOpQuickMatchQueuePort INSTANCE = new NoOpQuickMatchQueuePort();

  private NoOpQuickMatchQueuePort() {

  }

  @Override
  public void enqueue(final QuickMatchTicket ticket) {

  }

  @Override
  public Optional<QuickMatchTicket> tryDequeue(final PlayerId playerId) {

    return Optional.empty();
  }

  @Override
  public Optional<QuickMatchTicket> tryMatchOpponent(final PlayerId enqueuingPlayer,
      final GamesToPlay gamesToPlay) {

    return Optional.empty();
  }

  @Override
  public boolean isPlayerQueued(final PlayerId playerId) {

    return false;
  }

  @Override
  public Optional<QuickMatchTicket> findByPlayer(final PlayerId playerId) {

    return Optional.empty();
  }

  @Override
  public Optional<QuickMatchTicket> tryDequeueBySessionId(final String sessionId) {

    return Optional.empty();
  }

}
