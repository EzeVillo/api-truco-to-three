package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.application.timeout.CupTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.domain.ports.CupRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class CupTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final CupRepository cupRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final CupTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;

  public CupTimeoutReconciliationSource(final CupRepository cupRepository,
      final TimeoutActionDispatcher dispatcher, final CupTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout) {

    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return cupRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null)
        .filter(entry -> this.phasePolicy.phaseOf(entry.status()) == TimeoutPhase.LOBBY)
        .map(entry -> {
          final var key = TimeoutKey.of(EntityType.CUP, entry.cupId().value().toString());
          final var deadline = entry.lastActivityAt().plus(this.lobbyTimeout);
          return new TimeoutEntry(key, deadline, dispatcher.buildAction(key));
        });
  }

}
