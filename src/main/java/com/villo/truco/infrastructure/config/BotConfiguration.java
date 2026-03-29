package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.BotDomainEventTranslator;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.commands.CreateBotMatchCommandHandler;
import com.villo.truco.application.usecases.commands.ExecuteBotTurnCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.queries.GetBotsQueryHandler;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.infrastructure.bot.AsyncBotActionExecutor;
import com.villo.truco.infrastructure.bot.BotCatalogInitializer;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class BotConfiguration {

  private final BotRegistry botRegistry;
  private final MatchQueryRepository matchQueryRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PlayCardUseCase playCardUseCase;
  private final CallTrucoUseCase callTrucoUseCase;
  private final RespondTrucoUseCase respondTrucoUseCase;
  private final CallEnvidoUseCase callEnvidoUseCase;
  private final RespondEnvidoUseCase respondEnvidoUseCase;
  private final UseCasePipeline retryTransactionalPipeline;

  public BotConfiguration(final BotRegistry botRegistry,
      final MatchQueryRepository matchQueryRepository, final MatchRepository matchRepository,
      @Lazy final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      @Lazy final PlayCardUseCase playCardUseCase, @Lazy final CallTrucoUseCase callTrucoUseCase,
      @Lazy final RespondTrucoUseCase respondTrucoUseCase,
      @Lazy final CallEnvidoUseCase callEnvidoUseCase,
      @Lazy final RespondEnvidoUseCase respondEnvidoUseCase,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline) {

    this.botRegistry = botRegistry;
    this.matchQueryRepository = matchQueryRepository;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.playerAvailabilityChecker = playerAvailabilityChecker;
    this.playCardUseCase = playCardUseCase;
    this.callTrucoUseCase = callTrucoUseCase;
    this.respondTrucoUseCase = respondTrucoUseCase;
    this.callEnvidoUseCase = callEnvidoUseCase;
    this.respondEnvidoUseCase = respondEnvidoUseCase;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
  }

  @Bean
  Executor botExecutor() {

    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean
  ExecuteBotTurnUseCase executeBotTurnCommandHandler() {

    return new ExecuteBotTurnCommandHandler(this.botRegistry, this.matchQueryRepository,
        this.playCardUseCase, this.callTrucoUseCase, this.respondTrucoUseCase,
        this.callEnvidoUseCase, this.respondEnvidoUseCase);
  }

  @Bean
  AsyncBotActionExecutor asyncBotActionExecutor() {

    return new AsyncBotActionExecutor(this.executeBotTurnCommandHandler(), this.botExecutor());
  }

  @Bean
  BotDomainEventTranslator botDomainEventTranslator(final ApplicationEventPublisher publisher) {

    return new BotDomainEventTranslator(this.botRegistry, publisher);
  }

  @Bean
  CreateBotMatchUseCase createBotMatchCommandHandler() {

    final var handler = new CreateBotMatchCommandHandler(this.matchRepository,
        this.matchEventNotifier, this.botRegistry, this.playerAvailabilityChecker);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetBotsUseCase getBotsQueryHandler() {

    return new GetBotsQueryHandler(this.botRegistry);
  }

  @Bean
  BotCatalogInitializer botCatalogInitializer() {

    return new BotCatalogInitializer(this.botRegistry);
  }

}
