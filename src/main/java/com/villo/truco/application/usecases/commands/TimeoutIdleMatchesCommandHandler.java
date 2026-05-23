package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeoutIdleMatchesCommandHandler implements TimeoutIdleMatchesUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      TimeoutIdleMatchesCommandHandler.class);

  private final MatchQueryRepository matchQueryRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final RetryableTransactionalRunner transactionalRunner;

  public TimeoutIdleMatchesCommandHandler(final MatchQueryRepository matchQueryRepository,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final RetryableTransactionalRunner transactionalRunner) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
  }

  @Override
  public Void handle(final MatchId matchId) {

    try {
      this.transactionalRunner.run(() -> this.processIdleMatch(matchId));
    } catch (final Exception e) {
      LOGGER.error("Failed to process idle match: matchId={}", matchId, e);
    }
    return null;
  }

  private void processIdleMatch(final MatchId matchId) {

    final var matchOpt = this.matchQueryRepository.findById(matchId);
    if (matchOpt.isEmpty()) {
      return;
    }

    final var match = matchOpt.get();
    if (!match.timeoutForfeit()) {
      LOGGER.debug("Cannot process idle match, skipping: matchId={}", matchId);
      return;
    }

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();
  }

}
