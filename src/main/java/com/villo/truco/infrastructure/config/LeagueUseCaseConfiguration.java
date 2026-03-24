package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.application.usecases.commands.CreateLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.JoinLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.application.usecases.commands.LeaveLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.StartLeagueCommandHandler;
import com.villo.truco.application.usecases.queries.GetLeagueStateQueryHandler;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeagueUseCaseConfiguration {

  private final LeagueQueryRepository leagueQueryRepository;
  private final LeagueRepository leagueRepository;
  private final MatchRepository matchRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final UseCasePipeline retryTransactionalPipeline;
  private final UseCasePipeline transactionalPipeline;

  public LeagueUseCaseConfiguration(final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.leagueQueryRepository = leagueQueryRepository;
    this.leagueRepository = leagueRepository;
    this.matchRepository = matchRepository;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  LeagueResolver leagueResolver() {

    return new LeagueResolver(this.leagueQueryRepository);
  }

  @Bean
  CreateLeagueUseCase createLeagueCommandHandler() {

    final var handler = new CreateLeagueCommandHandler(this.leagueRepository,
        this.playerAvailabilityChecker);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinLeagueUseCase joinLeagueCommandHandler() {

    final var handler = new JoinLeagueCommandHandler(this.leagueResolver(), this.leagueRepository,
        this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartLeagueUseCase startLeagueCommandHandler() {

    final var handler = new StartLeagueCommandHandler(this.leagueResolver(), this.leagueRepository,
        this.matchRepository);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveLeagueUseCase leaveLeagueCommandHandler() {

    final var handler = new LeaveLeagueCommandHandler(this.leagueResolver(), this.leagueRepository);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetLeagueStateUseCase getLeagueStateQueryHandler() {

    return new GetLeagueStateQueryHandler(this.leagueResolver());
  }

}
