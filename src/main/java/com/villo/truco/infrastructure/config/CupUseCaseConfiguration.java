package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CupMatchAbandonedEventHandler;
import com.villo.truco.application.eventhandlers.CupMatchCompletedEventHandler;
import com.villo.truco.application.eventhandlers.CupMatchForfeitedEventHandler;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.ports.in.GetPublicCupsUseCase;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.application.ports.in.JoinPublicCupUseCase;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.application.usecases.commands.AdvanceCupCommandHandler;
import com.villo.truco.application.usecases.commands.CreateCupCommandHandler;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.application.usecases.commands.ForfeitCupCommandHandler;
import com.villo.truco.application.usecases.commands.JoinCupCommandHandler;
import com.villo.truco.application.usecases.commands.JoinPublicCupCommandHandler;
import com.villo.truco.application.usecases.commands.LeaveCupCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.StartCupCommandHandler;
import com.villo.truco.application.usecases.queries.GetCupStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetPublicCupsQueryHandler;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CupUseCaseConfiguration {

  private final CupQueryRepository cupQueryRepository;
  private final CupRepository cupRepository;
  private final MatchRepository matchRepository;
  private final CupEventNotifier cupEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PublicActorResolver publicActorResolver;
  private final UseCasePipeline retryTransactionalPipeline;
  private final UseCasePipeline transactionalPipeline;

  public CupUseCaseConfiguration(final CupQueryRepository cupQueryRepository,
      final CupRepository cupRepository, final MatchRepository matchRepository,
      @Lazy final CupEventNotifier cupEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final PublicActorResolver publicActorResolver,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.cupQueryRepository = cupQueryRepository;
    this.cupRepository = cupRepository;
    this.matchRepository = matchRepository;
    this.cupEventNotifier = cupEventNotifier;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.publicActorResolver = publicActorResolver;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  CupResolver cupResolver() {

    return new CupResolver(this.cupQueryRepository);
  }

  @Bean
  CreateCupUseCase createCupCommandHandler() {

    final var handler = new CreateCupCommandHandler(this.cupRepository, this.cupEventNotifier,
        this.playerAvailabilityChecker);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinCupUseCase joinCupCommandHandler() {

    final var handler = new JoinCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.playerAvailabilityChecker, this.cupEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinPublicCupUseCase joinPublicCupCommandHandler() {

    final var handler = new JoinPublicCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.matchRepository, this.cupEventNotifier, this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveCupUseCase leaveCupCommandHandler() {

    final var handler = new LeaveCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.cupEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartCupUseCase startCupCommandHandler() {

    final var handler = new StartCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.matchRepository, this.cupEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetCupStateUseCase getCupStateQueryHandler() {

    return new GetCupStateQueryHandler(this.cupResolver(), this.publicActorResolver);
  }

  @Bean
  GetPublicCupsUseCase getPublicCupsQueryHandler() {

    return new GetPublicCupsQueryHandler(this.cupQueryRepository, this.publicActorResolver,
        this.playerAvailabilityChecker);
  }

  @Bean
  AdvanceCupUseCase advanceCupCommandHandler() {

    final var handler = new AdvanceCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.matchRepository, this.cupEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  ForfeitCupUseCase forfeitCupCommandHandler() {

    final var handler = new ForfeitCupCommandHandler(this.cupResolver(), this.cupRepository,
        this.matchRepository, this.cupEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CupMatchCompletedEventHandler cupMatchCompletedHandler() {

    return new CupMatchCompletedEventHandler(this.cupQueryRepository, advanceCupCommandHandler());
  }

  @Bean
  CupMatchAbandonedEventHandler cupMatchAbandonedHandler() {

    return new CupMatchAbandonedEventHandler(this.cupQueryRepository, advanceCupCommandHandler());
  }

  @Bean
  CupMatchForfeitedEventHandler cupMatchForfeitedHandler() {

    return new CupMatchForfeitedEventHandler(this.cupQueryRepository, forfeitCupCommandHandler());
  }

}
