package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.domain.ports.MatchRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class MatchTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final MatchRepository matchRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final Duration idleTimeout;

  public MatchTimeoutReconciliationSource(final MatchRepository matchRepository,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return matchRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null).map(entry -> {
          final var key = TimeoutKey.of(EntityType.MATCH, entry.matchId().value().toString());
          final var deadline = entry.lastActivityAt().plus(idleTimeout);
          return new TimeoutEntry(key, deadline, dispatcher.buildAction(key));
        });
  }

}
