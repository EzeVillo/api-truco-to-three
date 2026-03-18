package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.LeagueMatchFinishedHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchForfeitedHandler;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.application.usecases.commands.AbandonMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CreateLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.JoinLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.LeaveLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.StartMatchCommandHandler;
import com.villo.truco.application.usecases.commands.StartLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleMatchesCommandHandler;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetLeagueStateQueryHandler;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.events.CompositeMatchEventNotifier;
import com.villo.truco.infrastructure.pipeline.OptimisticLockRetryBehavior;
import com.villo.truco.infrastructure.pipeline.TransactionalBehavior;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import com.villo.truco.infrastructure.websocket.StompMatchEventNotifier;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UseCaseConfiguration {

  @Bean
  TransactionTemplate transactionTemplate(final PlatformTransactionManager txManager) {

    return new TransactionTemplate(txManager);
  }

  @Bean
  OptimisticLockRetryBehavior optimisticLockRetryBehavior(
      @Value("${truco.retry.max-retries:3}") final int maxRetries,
      @Value("${truco.retry.delay-ms:200}") final long delayMs) {

    return new OptimisticLockRetryBehavior(maxRetries, Duration.ofMillis(delayMs));
  }

  @Bean
  TransactionalBehavior transactionalBehavior(final TransactionTemplate transactionTemplate) {

    return new TransactionalBehavior(transactionTemplate);
  }

  @Bean
  UseCasePipeline retryTransactionalPipeline(
      final OptimisticLockRetryBehavior optimisticLockRetryBehavior,
      final TransactionalBehavior transactionalBehavior) {

    return new UseCasePipeline(List.of(optimisticLockRetryBehavior, transactionalBehavior));
  }

  @Bean
  UseCasePipeline transactionalPipeline(final TransactionalBehavior transactionalBehavior) {

    return new UseCasePipeline(List.of(transactionalBehavior));
  }

  @Bean
  MatchResolver matchResolver(final MatchQueryRepository matchQueryRepository) {

    return new MatchResolver(matchQueryRepository);
  }

  @Bean
  RegisterUserUseCase registerUserCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new RegisterUserCommandHandler(userRepository, passwordHasher,
        tokenProvider);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LoginUseCase loginCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new LoginCommandHandler(userRepository, passwordHasher, tokenProvider);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GuestLoginUseCase guestLoginCommandHandler(final PlayerTokenProvider tokenProvider) {

    return new GuestLoginCommandHandler(tokenProvider);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler(final MatchRepository matchRepository,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateMatchCommandHandler(matchRepository);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinMatchCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartMatchUseCase startMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchQueryRepository matchQueryRepository,
      final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartMatchCommandHandler(matchResolver, matchRepository,
        matchQueryRepository, matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  PlayCardUseCase playCardCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new PlayCardCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CallTrucoUseCase callTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new CallTrucoCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RespondTrucoUseCase respondTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new RespondTrucoCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CallEnvidoUseCase callEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new CallEnvidoCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RespondEnvidoUseCase respondEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new RespondEnvidoCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  AbandonMatchUseCase abandonMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new AbandonMatchCommandHandler(matchResolver, matchRepository,
        matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  FoldUseCase foldCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new FoldCommandHandler(matchResolver, matchRepository, matchEventNotifier);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetMatchStateUseCase getMatchStateQueryHandler(final MatchQueryRepository matchQueryRepository) {

    return new GetMatchStateQueryHandler(matchQueryRepository);
  }

  @Bean
  LeagueResolver leagueResolver(final LeagueQueryRepository leagueQueryRepository) {

    return new LeagueResolver(leagueQueryRepository);
  }

  @Bean
  CreateLeagueUseCase createLeagueCommandHandler(
      final LeagueRepository leagueRepository,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateLeagueCommandHandler(leagueRepository);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinLeagueUseCase joinLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinLeagueCommandHandler(leagueResolver, leagueRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartLeagueUseCase startLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartLeagueCommandHandler(leagueResolver, leagueRepository,
        matchRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveLeagueUseCase leaveLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new LeaveLeagueCommandHandler(leagueResolver, leagueRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetLeagueStateUseCase getLeagueStateQueryHandler(
      final LeagueResolver leagueResolver) {

    return new GetLeagueStateQueryHandler(leagueResolver);
  }

  @Bean
  StompMatchEventNotifier stompMatchEventNotifier(final SimpMessagingTemplate messagingTemplate) {

    return new StompMatchEventNotifier(messagingTemplate);
  }

  @Bean
  LeagueMatchFinishedHandler leagueMatchFinishedHandler(
      final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository) {

    return new LeagueMatchFinishedHandler(leagueQueryRepository, leagueRepository);
  }

  @Bean
  LeagueMatchForfeitedHandler leagueMatchForfeitedHandler(
      final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository) {

    return new LeagueMatchForfeitedHandler(leagueQueryRepository, leagueRepository);
  }

  @Bean
  MatchEventNotifier matchEventNotifier(final StompMatchEventNotifier wsHandler,
      final LeagueMatchFinishedHandler finishedHandler,
      final LeagueMatchForfeitedHandler forfeitedHandler) {

    return new CompositeMatchEventNotifier(List.of(wsHandler, finishedHandler, forfeitedHandler));
  }

  @Bean
  TimeoutIdleMatchesUseCase timeoutIdleMatchesCommandHandler(
      final MatchQueryRepository matchQueryRepository, final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier,
      final com.villo.truco.application.ports.TransactionalRunner transactionalRunner,
      final MatchTimeoutProperties matchTimeoutProperties) {

    return new TimeoutIdleMatchesCommandHandler(matchQueryRepository, matchRepository,
        matchEventNotifier, transactionalRunner,
        Duration.ofSeconds(matchTimeoutProperties.getIdleTimeoutSeconds()));
  }

}
