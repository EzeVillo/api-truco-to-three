package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.application.timeout.LeagueTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.domain.ports.LeagueRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class LeagueTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final LeagueRepository leagueRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final LeagueTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;

  public LeagueTimeoutReconciliationSource(final LeagueRepository leagueRepository,
      final TimeoutActionDispatcher dispatcher, final LeagueTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout) {

    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return leagueRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null)
        .filter(entry -> this.phasePolicy.phaseOf(entry.status()) == TimeoutPhase.LOBBY)
        .map(entry -> {
          final var key = TimeoutKey.of(EntityType.LEAGUE, entry.leagueId().value().toString());
          final var deadline = entry.lastActivityAt().plus(this.lobbyTimeout);
          return new TimeoutEntry(key, deadline, dispatcher.buildAction(key));
        });
  }

}
