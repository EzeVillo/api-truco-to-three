package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.domain.ports.LeagueRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

public class LeagueTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final LeagueRepository leagueRepository;
  private final TimeoutActionDispatcher dispatcher;
  private final Duration idleTimeout;

  public LeagueTimeoutReconciliationSource(final LeagueRepository leagueRepository,
      final TimeoutActionDispatcher dispatcher, final Duration idleTimeout) {

    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.dispatcher = Objects.requireNonNull(dispatcher);
    this.idleTimeout = Objects.requireNonNull(idleTimeout);
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return leagueRepository.findActiveWithTimeoutDeadline()
        .filter(entry -> entry.lastActivityAt() != null).map(entry -> {
          final var key = TimeoutKey.of(EntityType.LEAGUE, entry.leagueId().value().toString());
          final var deadline = entry.lastActivityAt().plus(idleTimeout);
          return new TimeoutEntry(key, deadline, dispatcher.buildAction(key));
        });
  }

}
