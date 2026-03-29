package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeoutIdleCupsCommandHandler implements TimeoutIdleCupsUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutIdleCupsCommandHandler.class);

  private final CupQueryRepository cupQueryRepository;
  private final CupRepository cupRepository;
  private final TransactionalRunner transactionalRunner;
  private final Duration idleTimeout;
  private final CupEventNotifier cupEventNotifier;

  public TimeoutIdleCupsCommandHandler(final CupQueryRepository cupQueryRepository,
      final CupRepository cupRepository, final TransactionalRunner transactionalRunner,
      final Duration idleTimeout, final CupEventNotifier cupEventNotifier) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
  }

  @Override
  public void handle() {

    final var cutoff = Instant.now().minus(this.idleTimeout);
    final List<CupId> idleCupIds = this.cupQueryRepository.findIdleCupIds(cutoff);

    if (!idleCupIds.isEmpty()) {
      LOGGER.info("Found {} idle cups to process", idleCupIds.size());
    }

    for (final var cupId : idleCupIds) {
      try {
        this.transactionalRunner.run(() -> this.processIdleCup(cupId));
      } catch (final Exception e) {
        LOGGER.error("Failed to process idle cup: cupId={}", cupId, e);
      }
    }
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
