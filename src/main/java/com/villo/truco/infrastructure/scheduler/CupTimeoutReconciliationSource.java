package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.domain.ports.CupRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class CupTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final CupRepository cupRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final Duration idleTimeout;

  public CupTimeoutReconciliationSource(final CupRepository cupRepository,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return cupRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null).map(entry -> {
          final var key = TimeoutKey.of(EntityType.CUP, entry.cupId().value().toString());
          final var deadline = entry.lastActivityAt().plus(idleTimeout);
          return new TimeoutEntry(key, deadline, dispatcher.buildAction(key));
        });
  }

}
