package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.in.JoinByCodeUseCase;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.application.usecases.commands.JoinByCodeCommandHandler;
import com.villo.truco.application.usecases.commands.JoinTargetDispatcher;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.JoinCodeRegistryQueryRepository;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JoinUseCaseConfiguration {

  private final JoinCodeRegistryQueryRepository joinCodeRegistryQueryRepository;
  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final LeagueEventNotifier leagueEventNotifier;
  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final CupEventNotifier cupEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final UseCasePipeline retryTransactionalPipeline;

  public JoinUseCaseConfiguration(
      final JoinCodeRegistryQueryRepository joinCodeRegistryQueryRepository,
      final MatchResolver matchResolver, final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier, final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final LeagueEventNotifier leagueEventNotifier,
      final CupResolver cupResolver, final CupRepository cupRepository,
      final CupEventNotifier cupEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline) {

    this.joinCodeRegistryQueryRepository = joinCodeRegistryQueryRepository;
    this.matchResolver = matchResolver;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.leagueResolver = leagueResolver;
    this.leagueRepository = leagueRepository;
    this.leagueEventNotifier = leagueEventNotifier;
    this.cupResolver = cupResolver;
    this.cupRepository = cupRepository;
    this.cupEventNotifier = cupEventNotifier;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
  }

  @Bean
  JoinTargetDispatcher joinTargetDispatcher() {

    return new JoinTargetDispatcher(this.joinCodeRegistryQueryRepository, this.matchResolver,
        this.matchRepository, this.matchEventNotifier, this.leagueResolver, this.leagueRepository,
        this.leagueEventNotifier, this.cupResolver, this.cupRepository, this.cupEventNotifier,
        this.matchRepository, this.playerAvailabilityChecker);
  }

  @Bean
  JoinByCodeUseCase joinByCodeCommandHandler() {

    final var handler = new JoinByCodeCommandHandler(joinTargetDispatcher());
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

}
