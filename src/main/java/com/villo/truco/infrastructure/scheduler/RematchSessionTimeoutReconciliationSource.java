package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.util.stream.Stream;

public class RematchSessionTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final RematchSessionRepository repository;
  private final TimeoutActionDispatcher dispatcher;

  public RematchSessionTimeoutReconciliationSource(final RematchSessionRepository repository,
      final TimeoutActionDispatcher dispatcher) {

    this.repository = repository;
    this.dispatcher = dispatcher;
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return repository.findActiveWithExpiration().map(entry -> {
      final var key = TimeoutKey.of(EntityType.REMATCH_SESSION,
          entry.sessionId().value().toString());
      return new TimeoutEntry(key, entry.expiresAt(), dispatcher.buildAction(key));
    });
  }

}
