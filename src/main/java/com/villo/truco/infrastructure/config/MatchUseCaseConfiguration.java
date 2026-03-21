package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.usecases.commands.AbandonMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.StartMatchCommandHandler;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchUseCaseConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final UseCasePipeline retryTransactionalPipeline;
  private final UseCasePipeline transactionalPipeline;

  public MatchUseCaseConfiguration(final MatchQueryRepository matchQueryRepository,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.matchQueryRepository = matchQueryRepository;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  MatchResolver matchResolver() {

    return new MatchResolver(this.matchQueryRepository);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler() {

    final var handler = new CreateMatchCommandHandler(this.matchRepository,
        this.playerAvailabilityChecker);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler() {

    final var handler = new JoinMatchCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier, this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartMatchUseCase startMatchCommandHandler() {

    final var handler = new StartMatchCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier, this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  PlayCardUseCase playCardCommandHandler() {

    final var handler = new PlayCardCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CallTrucoUseCase callTrucoCommandHandler() {

    final var handler = new CallTrucoCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RespondTrucoUseCase respondTrucoCommandHandler() {

    final var handler = new RespondTrucoCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CallEnvidoUseCase callEnvidoCommandHandler() {

    final var handler = new CallEnvidoCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RespondEnvidoUseCase respondEnvidoCommandHandler() {

    final var handler = new RespondEnvidoCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  AbandonMatchUseCase abandonMatchCommandHandler() {

    final var handler = new AbandonMatchCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  FoldUseCase foldCommandHandler() {

    final var handler = new FoldCommandHandler(this.matchResolver(), this.matchRepository,
        this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetMatchStateUseCase getMatchStateQueryHandler() {

    return new GetMatchStateQueryHandler(this.matchQueryRepository);
  }

}
