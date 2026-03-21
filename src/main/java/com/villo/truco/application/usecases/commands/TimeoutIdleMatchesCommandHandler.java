package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
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
    if (match.isFinished()) {
      return;
    }

    if (match.getStatus() == MatchStatus.WAITING_FOR_PLAYERS) {
      match.cancel();
      this.matchRepository.save(match);
      this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
          match.getPlayerTwo(), match.getDomainEvents());
      match.clearDomainEvents();
      LOGGER.info("Match cancelled by timeout: matchId={}", matchId);
      return;
    }

    final var winner = determineWinner(match);
    if (winner == null) {
      LOGGER.debug("Cannot determine winner for idle match, skipping: matchId={}", matchId);
      return;
    }

    match.forfeit(winner);
    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    LOGGER.info("Match forfeited by timeout: matchId={}, winner={}", matchId, winner);
  }

  private PlayerId determineWinner(final Match match) {

    if (match.getStatus() == MatchStatus.IN_PROGRESS) {
      final var currentTurn = match.getCurrentTurn();
      if (currentTurn == null) {
        return null;
      }
      return currentTurn.equals(match.getPlayerOne()) ? match.getPlayerTwo() : match.getPlayerOne();
    }

    if (match.getStatus() == MatchStatus.READY) {
      if (match.getPlayerTwo() == null) {
        return null;
      }
      if (!match.isReadyPlayerOne()) {
        return match.getPlayerTwo();
      }
      if (!match.isReadyPlayerTwo()) {
        return match.getPlayerOne();
      }
      return match.getPlayerTwo();
    }

    return null;
  }

}
