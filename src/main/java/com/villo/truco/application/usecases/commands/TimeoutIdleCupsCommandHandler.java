package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeoutIdleCupsCommandHandler implements TimeoutIdleCupsUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutIdleCupsCommandHandler.class);

  private final CupQueryRepository cupQueryRepository;
  private final CupRepository cupRepository;
  private final RetryableTransactionalRunner transactionalRunner;
  private final CupEventNotifier cupEventNotifier;

  public TimeoutIdleCupsCommandHandler(final CupQueryRepository cupQueryRepository,
      final CupRepository cupRepository, final RetryableTransactionalRunner transactionalRunner,
      final CupEventNotifier cupEventNotifier) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
  }

  @Override
  public Void handle(final CupId cupId) {

    try {
      this.transactionalRunner.run(() -> this.processIdleCup(cupId));
    } catch (final Exception e) {
      LOGGER.error("Failed to process idle cup: cupId={}", cupId, e);
    }
    return null;
  }

  private void processIdleCup(final CupId cupId) {

    final var cupOpt = this.cupQueryRepository.findById(cupId);
    if (cupOpt.isEmpty()) {
      return;
    }

    final var cup = cupOpt.get();
    final var statusBefore = cup.getStatus();

    cup.cancel();
    if (cup.getStatus() == statusBefore) {
      return;
    }

    this.cupRepository.save(cup);
    LOGGER.info("Cup cancelled by timeout: cupId={}", cupId);

    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());
    cup.clearDomainEvents();
  }

}
