package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.LeagueMatchAbandonedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchCompletedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchForfeitedEventHandler;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.GetPublicLeaguesUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.JoinPublicLeagueUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.application.usecases.commands.AdvanceLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.CreateLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.ForfeitLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.JoinLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.JoinPublicLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.application.usecases.commands.LeaveLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.StartLeagueCommandHandler;
import com.villo.truco.application.usecases.queries.GetLeagueStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetPublicLeaguesQueryHandler;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class LeagueUseCaseConfiguration {

  private final LeagueQueryRepository leagueQueryRepository;
  private final LeagueRepository leagueRepository;
  private final MatchRepository matchRepository;
  private final LeagueEventNotifier leagueEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PublicActorResolver publicActorResolver;
  private final UseCasePipeline retryTransactionalPipeline;
  private final UseCasePipeline transactionalPipeline;

  public LeagueUseCaseConfiguration(final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      @Lazy final LeagueEventNotifier leagueEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final PublicActorResolver publicActorResolver,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.leagueQueryRepository = leagueQueryRepository;
    this.leagueRepository = leagueRepository;
    this.matchRepository = matchRepository;
    this.leagueEventNotifier = leagueEventNotifier;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.publicActorResolver = publicActorResolver;
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
        this.leagueEventNotifier, this.playerAvailabilityChecker);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinLeagueUseCase joinLeagueCommandHandler() {

    final var handler = new JoinLeagueCommandHandler(this.leagueResolver(), this.leagueRepository,
        this.playerAvailabilityChecker, this.leagueEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinPublicLeagueUseCase joinPublicLeagueCommandHandler() {

    final var handler = new JoinPublicLeagueCommandHandler(this.leagueResolver(),
        this.leagueRepository, this.matchRepository, this.leagueEventNotifier,
        this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartLeagueUseCase startLeagueCommandHandler() {

    final var handler = new StartLeagueCommandHandler(this.leagueResolver(), this.leagueRepository,
        this.matchRepository, this.leagueEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveLeagueUseCase leaveLeagueCommandHandler() {

    final var handler = new LeaveLeagueCommandHandler(this.leagueResolver(), this.leagueRepository,
        this.leagueEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  AdvanceLeagueUseCase advanceLeagueCommandHandler() {

    final var handler = new AdvanceLeagueCommandHandler(this.leagueResolver(),
        this.leagueRepository, this.matchRepository, this.leagueEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  ForfeitLeagueUseCase forfeitLeagueCommandHandler() {

    final var handler = new ForfeitLeagueCommandHandler(this.leagueResolver(),
        this.leagueRepository, this.matchRepository, this.leagueEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetLeagueStateUseCase getLeagueStateQueryHandler() {

    return new GetLeagueStateQueryHandler(this.leagueResolver(), this.publicActorResolver);
  }

  @Bean
  GetPublicLeaguesUseCase getPublicLeaguesQueryHandler() {

    return new GetPublicLeaguesQueryHandler(this.leagueQueryRepository, this.publicActorResolver,
        this.playerAvailabilityChecker);
  }

  @Bean
  LeagueMatchCompletedEventHandler leagueMatchCompletedHandler() {

    return new LeagueMatchCompletedEventHandler(this.leagueQueryRepository,
        advanceLeagueCommandHandler());
  }

  @Bean
  LeagueMatchAbandonedEventHandler leagueMatchAbandonedHandler() {

    return new LeagueMatchAbandonedEventHandler(this.leagueQueryRepository,
        advanceLeagueCommandHandler());
  }

  @Bean
  LeagueMatchForfeitedEventHandler leagueMatchForfeitedHandler() {

    return new LeagueMatchForfeitedEventHandler(this.leagueQueryRepository,
        forfeitLeagueCommandHandler());
  }

}
