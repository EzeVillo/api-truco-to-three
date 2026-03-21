package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeoutIdleMatchesCommandHandler implements TimeoutIdleMatchesUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      TimeoutIdleMatchesCommandHandler.class);

  private final MatchQueryRepository matchQueryRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final TransactionalRunner transactionalRunner;
  private final Duration idleTimeout;

  public TimeoutIdleMatchesCommandHandler(final MatchQueryRepository matchQueryRepository,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final TransactionalRunner transactionalRunner, final Duration idleTimeout) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
  }

  @Override
  public void handle() {

    final var cutoff = Instant.now().minus(this.idleTimeout);
    final List<MatchId> idleMatchIds = this.matchQueryRepository.findIdleMatchIds(cutoff);

    if (!idleMatchIds.isEmpty()) {
      LOGGER.info("Found {} idle matches to process", idleMatchIds.size());
    }

    for (final var matchId : idleMatchIds) {
      try {
        this.transactionalRunner.run(() -> this.processIdleMatch(matchId));
      } catch (final Exception e) {
        LOGGER.error("Failed to process idle match: matchId={}", matchId, e);
      }
    }
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
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();
  }

}
