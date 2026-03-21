package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CupMatchFinishedEventHandler;
import com.villo.truco.application.eventhandlers.CupMatchForfeitedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchFinishedEventHandler;
import com.villo.truco.application.eventhandlers.LeagueMatchForfeitedEventHandler;
import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleLeaguesUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.application.usecases.commands.AbandonMatchCommandHandler;
import com.villo.truco.application.usecases.commands.AdvanceCupCommandHandler;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateCupCommandHandler;
import com.villo.truco.application.usecases.commands.CreateLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.ForfeitCupCommandHandler;
import com.villo.truco.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.application.usecases.commands.JoinCupCommandHandler;
import com.villo.truco.application.usecases.commands.JoinLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.application.usecases.commands.LeaveCupCommandHandler;
import com.villo.truco.application.usecases.commands.LeaveLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.StartCupCommandHandler;
import com.villo.truco.application.usecases.commands.StartLeagueCommandHandler;
import com.villo.truco.application.usecases.commands.StartMatchCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleCupsCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleLeaguesCommandHandler;
import com.villo.truco.application.usecases.commands.TimeoutIdleMatchesCommandHandler;
import com.villo.truco.application.usecases.queries.GetCupStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetLeagueStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
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
  PlayerAvailabilityChecker playerAvailabilityChecker(
      final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository) {

    return new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler(final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateMatchCommandHandler(matchRepository, playerAvailabilityChecker);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinMatchCommandHandler(matchResolver, matchRepository,
        matchEventNotifier, playerAvailabilityChecker);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartMatchUseCase startMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartMatchCommandHandler(matchResolver, matchRepository,
        matchEventNotifier, playerAvailabilityChecker);
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
  CreateLeagueUseCase createLeagueCommandHandler(final LeagueRepository leagueRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateLeagueCommandHandler(leagueRepository, playerAvailabilityChecker);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinLeagueUseCase joinLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinLeagueCommandHandler(leagueResolver, leagueRepository,
        playerAvailabilityChecker);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartLeagueUseCase startLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartLeagueCommandHandler(leagueResolver, leagueRepository,
        matchRepository, playerAvailabilityChecker);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveLeagueUseCase leaveLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new LeaveLeagueCommandHandler(leagueResolver, leagueRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetLeagueStateUseCase getLeagueStateQueryHandler(final LeagueResolver leagueResolver) {

    return new GetLeagueStateQueryHandler(leagueResolver);
  }

  @Bean
  StompMatchEventNotifier stompMatchEventNotifier(final SimpMessagingTemplate messagingTemplate) {

    return new StompMatchEventNotifier(messagingTemplate);
  }

  @Bean
  LeagueMatchFinishedEventHandler leagueMatchFinishedHandler(
      final LeagueQueryRepository leagueQueryRepository, final LeagueRepository leagueRepository) {

    return new LeagueMatchFinishedEventHandler(leagueQueryRepository, leagueRepository);
  }

  @Bean
  LeagueMatchForfeitedEventHandler leagueMatchForfeitedHandler(
      final LeagueQueryRepository leagueQueryRepository, final LeagueRepository leagueRepository) {

    return new LeagueMatchForfeitedEventHandler(leagueQueryRepository, leagueRepository);
  }

  @Bean
  CupResolver cupResolver(final CupQueryRepository cupQueryRepository) {

    return new CupResolver(cupQueryRepository);
  }

  @Bean
  CreateCupUseCase createCupCommandHandler(final CupRepository cupRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateCupCommandHandler(cupRepository, playerAvailabilityChecker);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinCupUseCase joinCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinCupCommandHandler(cupResolver, cupRepository,
        playerAvailabilityChecker);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveCupUseCase leaveCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new LeaveCupCommandHandler(cupResolver, cupRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartCupUseCase startCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartCupCommandHandler(cupResolver, cupRepository, matchRepository,
        playerAvailabilityChecker);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetCupStateUseCase getCupStateQueryHandler(final CupResolver cupResolver) {

    return new GetCupStateQueryHandler(cupResolver);
  }

  @Bean
  AdvanceCupUseCase advanceCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final MatchRepository matchRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new AdvanceCupCommandHandler(cupResolver, cupRepository, matchRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  ForfeitCupUseCase forfeitCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final MatchRepository matchRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new ForfeitCupCommandHandler(cupResolver, cupRepository, matchRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  CupMatchFinishedEventHandler cupMatchFinishedHandler(final CupQueryRepository cupQueryRepository,
      final AdvanceCupUseCase advanceCupUseCase) {

    return new CupMatchFinishedEventHandler(cupQueryRepository, advanceCupUseCase);
  }

  @Bean
  CupMatchForfeitedEventHandler cupMatchForfeitedHandler(
      final CupQueryRepository cupQueryRepository, final ForfeitCupUseCase forfeitCupUseCase) {

    return new CupMatchForfeitedEventHandler(cupQueryRepository, forfeitCupUseCase);
  }

  @Bean
  MatchEventNotifier matchEventNotifier(final StompMatchEventNotifier wsHandler,
      final LeagueMatchFinishedEventHandler leagueFinishedHandler,
      final LeagueMatchForfeitedEventHandler leagueForfeitedHandler,
      final CupMatchFinishedEventHandler cupFinishedHandler,
      final CupMatchForfeitedEventHandler cupForfeitedHandler) {

    return new CompositeMatchEventNotifier(
        List.of(wsHandler, leagueFinishedHandler, leagueForfeitedHandler, cupFinishedHandler,
            cupForfeitedHandler));
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

  @Bean
  TimeoutIdleLeaguesUseCase timeoutIdleLeaguesCommandHandler(
      final LeagueQueryRepository leagueQueryRepository, final LeagueRepository leagueRepository,
      final com.villo.truco.application.ports.TransactionalRunner transactionalRunner,
      final LeagueTimeoutProperties leagueTimeoutProperties) {

    return new TimeoutIdleLeaguesCommandHandler(leagueQueryRepository, leagueRepository,
        transactionalRunner, Duration.ofSeconds(leagueTimeoutProperties.getIdleTimeoutSeconds()));
  }

  @Bean
  TimeoutIdleCupsUseCase timeoutIdleCupsCommandHandler(final CupQueryRepository cupQueryRepository,
      final CupRepository cupRepository,
      final com.villo.truco.application.ports.TransactionalRunner transactionalRunner,
      final CupTimeoutProperties cupTimeoutProperties) {

    return new TimeoutIdleCupsCommandHandler(cupQueryRepository, cupRepository, transactionalRunner,
        Duration.ofSeconds(cupTimeoutProperties.getIdleTimeoutSeconds()));
  }

}
