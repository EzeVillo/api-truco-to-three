package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleLeaguesUseCase;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeoutIdleLeaguesCommandHandler implements TimeoutIdleLeaguesUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      TimeoutIdleLeaguesCommandHandler.class);

  private final LeagueQueryRepository leagueQueryRepository;
  private final LeagueRepository leagueRepository;
  private final TransactionalRunner transactionalRunner;
  private final Duration idleTimeout;
  private final LeagueEventNotifier leagueEventNotifier;

  public TimeoutIdleLeaguesCommandHandler(final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository, final TransactionalRunner transactionalRunner,
      final Duration idleTimeout, final LeagueEventNotifier leagueEventNotifier) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
  }

  @Override
  public void handle() {

    final var cutoff = Instant.now().minus(this.idleTimeout);
    final List<LeagueId> idleLeagueIds = this.leagueQueryRepository.findIdleLeagueIds(cutoff);

    if (!idleLeagueIds.isEmpty()) {
      LOGGER.info("Found {} idle leagues to process", idleLeagueIds.size());
    }

    for (final var leagueId : idleLeagueIds) {
      try {
        this.transactionalRunner.run(() -> this.processIdleLeague(leagueId));
      } catch (final Exception e) {
        LOGGER.error("Failed to process idle league: leagueId={}", leagueId, e);
      }
    }
  }

  private void processIdleLeague(final LeagueId leagueId) {

    final var leagueOpt = this.leagueQueryRepository.findById(leagueId);
    if (leagueOpt.isEmpty()) {
      return;
    }

    final var league = leagueOpt.get();
    final var statusBefore = league.getStatus();

    final var participants = List.copyOf(league.getParticipants());

    league.cancel();
    if (league.getStatus() == statusBefore) {
      return;
    }

    this.leagueRepository.save(league);
    LOGGER.info("League cancelled by timeout: leagueId={}", leagueId);

    this.leagueEventNotifier.publishDomainEvents(leagueId, participants, league.getDomainEvents());
    league.clearDomainEvents();
  }

}
