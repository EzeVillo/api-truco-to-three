package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleLeaguesUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.application.usecases.commands.TimeoutIdleCupsCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleLeaguesCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleMatchesCommandHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutUseCaseConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final LeagueQueryRepository leagueQueryRepository;
  private final LeagueRepository leagueRepository;
  private final CupQueryRepository cupQueryRepository;
  private final CupRepository cupRepository;
  private final TransactionalRunner transactionalRunner;
  private final MatchTimeoutProperties matchTimeoutProperties;
  private final LeagueTimeoutProperties leagueTimeoutProperties;
  private final CupTimeoutProperties cupTimeoutProperties;

  public TimeoutUseCaseConfiguration(final MatchQueryRepository matchQueryRepository,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final LeagueQueryRepository leagueQueryRepository, final LeagueRepository leagueRepository,
      final CupQueryRepository cupQueryRepository, final CupRepository cupRepository,
      final TransactionalRunner transactionalRunner,
      final MatchTimeoutProperties matchTimeoutProperties,
      final LeagueTimeoutProperties leagueTimeoutProperties,
      final CupTimeoutProperties cupTimeoutProperties) {

    this.matchQueryRepository = matchQueryRepository;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.leagueQueryRepository = leagueQueryRepository;
    this.leagueRepository = leagueRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.cupRepository = cupRepository;
    this.transactionalRunner = transactionalRunner;
    this.matchTimeoutProperties = matchTimeoutProperties;
    this.leagueTimeoutProperties = leagueTimeoutProperties;
    this.cupTimeoutProperties = cupTimeoutProperties;
  }

  @Bean
  TimeoutIdleMatchesUseCase timeoutIdleMatchesCommandHandler() {

    return new TimeoutIdleMatchesCommandHandler(this.matchQueryRepository, this.matchRepository,
        this.matchEventNotifier, this.transactionalRunner,
        Duration.ofSeconds(this.matchTimeoutProperties.getIdleTimeoutSeconds()));
  }

  @Bean
  TimeoutIdleLeaguesUseCase timeoutIdleLeaguesCommandHandler() {

    return new TimeoutIdleLeaguesCommandHandler(this.leagueQueryRepository, this.leagueRepository,
        this.transactionalRunner,
        Duration.ofSeconds(this.leagueTimeoutProperties.getIdleTimeoutSeconds()));
  }

  @Bean
  TimeoutIdleCupsUseCase timeoutIdleCupsCommandHandler() {

    return new TimeoutIdleCupsCommandHandler(this.cupQueryRepository, this.cupRepository,
        this.transactionalRunner,
        Duration.ofSeconds(this.cupTimeoutProperties.getIdleTimeoutSeconds()));
  }

}
