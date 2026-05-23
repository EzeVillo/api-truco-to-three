package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.application.ports.in.EnqueueForQuickMatchUseCase;
import com.villo.truco.application.usecases.commands.CancelQuickMatchSearchCommandHandler;
import com.villo.truco.application.usecases.commands.EnqueueForQuickMatchCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import com.villo.truco.infrastructure.websocket.QuickMatchSessionDisconnectEventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuickMatchUseCaseConfiguration {

  private final QuickMatchQueuePort quickMatchQueuePort;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final UseCasePipeline retryTransactionalPipeline;

  public QuickMatchUseCaseConfiguration(final QuickMatchQueuePort quickMatchQueuePort,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline) {

    this.quickMatchQueuePort = quickMatchQueuePort;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
  }

  @Bean
  EnqueueForQuickMatchUseCase enqueueForQuickMatchCommandHandler() {

    final var handler = new EnqueueForQuickMatchCommandHandler(this.quickMatchQueuePort,
        this.playerAvailabilityChecker, this.matchRepository, this.matchEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CancelQuickMatchSearchUseCase cancelQuickMatchSearchCommandHandler() {

    final var handler = new CancelQuickMatchSearchCommandHandler(this.quickMatchQueuePort);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  QuickMatchSessionDisconnectEventListener quickMatchSessionDisconnectEventListener() {

    return new QuickMatchSessionDisconnectEventListener(cancelQuickMatchSearchCommandHandler());
  }

}
