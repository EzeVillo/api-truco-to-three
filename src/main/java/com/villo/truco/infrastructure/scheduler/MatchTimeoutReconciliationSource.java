package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.application.timeout.MatchTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.domain.ports.MatchRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class MatchTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final MatchRepository matchRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final MatchTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;
  private final Duration playTimeout;

  public MatchTimeoutReconciliationSource(final MatchRepository matchRepository,
      final TimeoutActionDispatcher dispatcher, final MatchTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout, final Duration playTimeout) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
    this.playTimeout = Objects.requireNonNull(playTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return matchRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null).flatMap(entry -> {
          final var timeout = timeoutFor(this.phasePolicy.phaseOf(entry.status()));
          if (timeout == null) {
            return Stream.empty();
          }
          final var key = TimeoutKey.of(EntityType.MATCH, entry.matchId().value().toString());
          final var deadline = entry.lastActivityAt().plus(timeout);
          return Stream.of(new TimeoutEntry(key, deadline, dispatcher.buildAction(key)));
        });
  }

  private Duration timeoutFor(final TimeoutPhase phase) {

    return switch (phase) {
      case LOBBY -> this.lobbyTimeout;
      case PLAY -> this.playTimeout;
      case NONE -> null;
    };
  }

}
