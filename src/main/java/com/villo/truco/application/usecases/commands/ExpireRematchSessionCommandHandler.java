package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.application.ports.in.ExpireRematchSessionUseCase;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpireRematchSessionCommandHandler implements ExpireRematchSessionUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ExpireRematchSessionCommandHandler.class);

  private final RematchSessionRepository repository;
  private final RematchSessionEventNotifier eventNotifier;
  private final RetryableTransactionalRunner transactionalRunner;
  private final Clock clock;

  public ExpireRematchSessionCommandHandler(final RematchSessionRepository repository,
      final RematchSessionEventNotifier eventNotifier,
      final RetryableTransactionalRunner transactionalRunner, final Clock clock) {

    this.repository = Objects.requireNonNull(repository);
    this.eventNotifier = Objects.requireNonNull(eventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Void handle(final RematchSessionId id) {

    try {
      this.transactionalRunner.run(() -> this.processExpiration(id));
    } catch (final Exception e) {
      LOGGER.error("Failed to expire rematch session: sessionId={}", id, e);
    }
    return null;
  }

  private void processExpiration(final RematchSessionId id) {

    final var session = this.repository.findById(id).orElse(null);
    if (session == null) {
      return;
    }
    session.expireIfNeeded(this.clock.instant());
    this.repository.save(session);
    this.eventNotifier.publishDomainEvents(session.getRematchDomainEvents());
    session.clearDomainEvents();
  }

}
