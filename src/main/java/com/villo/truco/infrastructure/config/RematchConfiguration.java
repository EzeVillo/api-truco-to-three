package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.MatchFinishedRematchSessionCreator;
import com.villo.truco.application.eventhandlers.RematchNotificationEventTranslator;
import com.villo.truco.application.eventhandlers.RematchPresenceEventTranslator;
import com.villo.truco.application.eventhandlers.RematchSessionConfirmedMatchCreator;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.RematchVeto;
import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.application.ports.in.ChooseRematchUseCase;
import com.villo.truco.application.ports.in.ExpireRematchSessionUseCase;
import com.villo.truco.application.ports.in.GetRematchSessionUseCase;
import com.villo.truco.application.ports.in.LeaveRematchUseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.application.usecases.commands.ChooseRematchCommandHandler;
import com.villo.truco.application.usecases.commands.ExpireDueRematchSessionsCommandHandler;
import com.villo.truco.application.usecases.commands.ExpireRematchSessionCommandHandler;
import com.villo.truco.application.usecases.commands.LeaveRematchCommandHandler;
import com.villo.truco.application.usecases.commands.RematchEligibilityPolicy;
import com.villo.truco.application.usecases.queries.GetRematchSessionQueryHandler;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.infrastructure.events.RematchSessionDomainEventDispatcher;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties(RematchExpirationProperties.class)
public class RematchConfiguration {

  private final RematchSessionRepository rematchSessionRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final BotRegistry botRegistry;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final PublicActorResolver publicActorResolver;
  private final RematchExpirationProperties properties;
  private final Clock clock;
  private final UseCasePipeline retryTransactionalPipeline;
  private final RetryableTransactionalRunner retryableTransactionalRunner;

  public RematchConfiguration(final RematchSessionRepository rematchSessionRepository,
      final MatchRepository matchRepository, @Lazy final MatchEventNotifier matchEventNotifier,
      final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final BotRegistry botRegistry,
      final ApplicationEventPublisher applicationEventPublisher,
      final PublicActorResolver publicActorResolver, final RematchExpirationProperties properties,
      final Clock clock,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline,
      final RetryableTransactionalRunner retryableTransactionalRunner) {

    this.rematchSessionRepository = rematchSessionRepository;
    this.matchRepository = matchRepository;
    this.matchEventNotifier = matchEventNotifier;
    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.botRegistry = botRegistry;
    this.applicationEventPublisher = applicationEventPublisher;
    this.publicActorResolver = publicActorResolver;
    this.properties = properties;
    this.clock = clock;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
    this.retryableTransactionalRunner = retryableTransactionalRunner;
  }

  @Bean
  RematchEligibilityPolicy rematchEligibilityPolicy(final List<RematchVeto> rematchVetoes) {

    return new RematchEligibilityPolicy(this.leagueQueryRepository, this.cupQueryRepository,
        rematchVetoes);
  }

  @Bean
  RematchNotificationEventTranslator rematchNotificationEventTranslator() {

    return new RematchNotificationEventTranslator(this.applicationEventPublisher,
        this.publicActorResolver);
  }

  @Bean
  RematchSessionConfirmedMatchCreator rematchSessionConfirmedMatchCreator(
      @Lazy final RematchSessionEventNotifier rematchSessionEventNotifier) {

    return new RematchSessionConfirmedMatchCreator(this.matchRepository, this.matchEventNotifier,
        this.rematchSessionRepository, rematchSessionEventNotifier, this.botRegistry);
  }

  @Bean
  RematchSessionEventNotifier rematchSessionEventNotifier(
      final RematchSessionConfirmedMatchCreator rematchSessionConfirmedMatchCreator,
      final RematchSessionDomainEventHandler<RematchSessionDomainEvent> rematchSessionTimeoutEventHandler,
      final RematchPresenceEventTranslator rematchPresenceEventTranslator) {

    final List<RematchSessionDomainEventHandler<?>> handlers = List.of(
        rematchNotificationEventTranslator(), rematchSessionConfirmedMatchCreator,
        rematchSessionTimeoutEventHandler, rematchPresenceEventTranslator);
    return new RematchSessionDomainEventDispatcher(handlers);
  }

  @Bean
  ExpireRematchSessionUseCase expireRematchSessionCommandHandler(
      @Lazy final RematchSessionEventNotifier rematchSessionEventNotifier) {

    return new ExpireRematchSessionCommandHandler(this.rematchSessionRepository,
        rematchSessionEventNotifier, this.retryableTransactionalRunner, this.clock);
  }

  @Bean
  MatchFinishedRematchSessionCreator matchFinishedRematchSessionCreator(
      final RematchSessionEventNotifier rematchSessionEventNotifier,
      final RematchEligibilityPolicy rematchEligibilityPolicy) {

    return new MatchFinishedRematchSessionCreator(this.rematchSessionRepository,
        rematchSessionEventNotifier, rematchEligibilityPolicy, this.botRegistry,
        this.matchQueryRepository, this.properties.duration(), this.clock);
  }

  @Bean
  ChooseRematchUseCase chooseRematchCommandHandler(
      final RematchSessionEventNotifier rematchSessionEventNotifier) {

    final var handler = new ChooseRematchCommandHandler(this.rematchSessionRepository,
        rematchSessionEventNotifier, this.clock);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveRematchUseCase leaveRematchCommandHandler(
      final RematchSessionEventNotifier rematchSessionEventNotifier) {

    final var handler = new LeaveRematchCommandHandler(this.rematchSessionRepository,
        rematchSessionEventNotifier);
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetRematchSessionUseCase getRematchSessionQueryHandler() {

    return new GetRematchSessionQueryHandler(this.rematchSessionRepository);
  }

  @Bean
  ExpireDueRematchSessionsCommandHandler expireDueRematchSessionsCommandHandler(
      final RematchSessionEventNotifier rematchSessionEventNotifier) {

    return new ExpireDueRematchSessionsCommandHandler(this.rematchSessionRepository,
        rematchSessionEventNotifier, this.retryableTransactionalRunner, this.clock,
        this.properties.batchSize());
  }

}
