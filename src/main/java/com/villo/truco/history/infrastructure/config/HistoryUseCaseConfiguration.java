package com.villo.truco.history.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.history.application.services.MatchHistoryTrackingService;
import com.villo.truco.history.application.usecases.queries.GetPlayerMatchHistoryQueryHandler;
import com.villo.truco.history.application.usecases.queries.GetPlayerMatchHistoryUseCase;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import com.villo.truco.history.infrastructure.eventhandlers.HistoryMatchDomainEventHandler;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HistoryUseCaseConfiguration {

  private final UserQueryRepository userQueryRepository;
  private final PlayerMatchHistoryRepository playerMatchHistoryRepository;
  private final BotRegistry botRegistry;
  private final UseCasePipeline transactionalPipeline;

  public HistoryUseCaseConfiguration(final UserQueryRepository userQueryRepository,
      final PlayerMatchHistoryRepository playerMatchHistoryRepository,
      final BotRegistry botRegistry,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.userQueryRepository = userQueryRepository;
    this.playerMatchHistoryRepository = playerMatchHistoryRepository;
    this.botRegistry = botRegistry;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  MatchHistoryTrackingService matchHistoryTrackingService() {

    return new MatchHistoryTrackingService(this.userQueryRepository,
        this.playerMatchHistoryRepository);
  }

  @Bean
  HistoryMatchDomainEventHandler historyMatchDomainEventHandler(
      final MatchHistoryTrackingService matchHistoryTrackingService) {

    return new HistoryMatchDomainEventHandler(matchHistoryTrackingService);
  }

  @Bean
  GetPlayerMatchHistoryUseCase getPlayerMatchHistoryUseCase() {

    final var handler = new GetPlayerMatchHistoryQueryHandler(this.playerMatchHistoryRepository,
        this.userQueryRepository, this.botRegistry);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

}
