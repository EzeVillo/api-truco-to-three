package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpireDueRematchSessionsCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ExpireDueRematchSessionsCommandHandler.class);

  private final RematchSessionRepository repository;
  private final RematchSessionEventNotifier eventNotifier;
  private final RetryableTransactionalRunner retryableRunner;
  private final Clock clock;
  private final int batchSize;

  public ExpireDueRematchSessionsCommandHandler(final RematchSessionRepository repository,
      final RematchSessionEventNotifier eventNotifier,
      final RetryableTransactionalRunner retryableRunner, final Clock clock, final int batchSize) {

    this.repository = Objects.requireNonNull(repository);
    this.eventNotifier = Objects.requireNonNull(eventNotifier);
    this.retryableRunner = Objects.requireNonNull(retryableRunner);
    this.clock = Objects.requireNonNull(clock);
    this.batchSize = batchSize;
  }

  public void expireAll() {

    final var now = clock.instant();
    final var candidates = repository.findExpiredCandidates(now, batchSize);
    LOGGER.debug("Expiring {} rematch session(s)", candidates.size());

    for (final var candidate : candidates) {
      final var sessionId = candidate.getId();
      try {
        this.retryableRunner.run(() -> {
          final var session = repository.findById(sessionId).orElse(null);
          if (session == null) {
            return;
          }
          session.expireIfNeeded(now);
          repository.save(session);
          eventNotifier.publishDomainEvents(session.getRematchDomainEvents());
          session.clearDomainEvents();
        });
      } catch (final Exception e) {
        LOGGER.error("Failed to expire rematch session: sessionId={}", sessionId, e);
      }
    }
  }

}
