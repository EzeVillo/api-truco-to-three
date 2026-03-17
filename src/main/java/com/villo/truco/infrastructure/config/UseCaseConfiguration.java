package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.PasswordHasher;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.JoinTournamentUseCase;
import com.villo.truco.application.ports.in.LeaveTournamentUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.ports.in.StartTournamentUseCase;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CreateTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.GuestLoginCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.JoinTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.LeaveTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.LoginCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterTournamentMatchResultCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterUserCommandHandler;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.StartMatchCommandHandler;
import com.villo.truco.application.usecases.commands.StartTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.TournamentResolver;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetTournamentStateQueryHandler;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.pipeline.OptimisticLockRetryBehavior;
import com.villo.truco.infrastructure.pipeline.TransactionalBehavior;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  TournamentResolver tournamentResolver(final TournamentQueryRepository tournamentQueryRepository) {

    return new TournamentResolver(tournamentQueryRepository);
  }

  @Bean
  CreateTournamentUseCase createTournamentCommandHandler(
      final TournamentRepository tournamentRepository,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new CreateTournamentCommandHandler(tournamentRepository);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  JoinTournamentUseCase joinTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new JoinTournamentCommandHandler(tournamentResolver, tournamentRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  StartTournamentUseCase startTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository, final MatchRepository matchRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new StartTournamentCommandHandler(tournamentResolver, tournamentRepository,
        matchRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  LeaveTournamentUseCase leaveTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final UseCasePipeline retryTransactionalPipeline) {

    final var handler = new LeaveTournamentCommandHandler(tournamentResolver, tournamentRepository);
    return retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  RegisterTournamentMatchResultUseCase registerTournamentMatchResultCommandHandler(
      final TournamentResolver tournamentResolver, final TournamentRepository tournamentRepository,
      final MatchQueryRepository matchQueryRepository,
      final UseCasePipeline transactionalPipeline) {

    final var handler = new RegisterTournamentMatchResultCommandHandler(tournamentResolver,
        tournamentRepository, matchQueryRepository);
    return transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetTournamentStateUseCase getTournamentStateQueryHandler(
      final TournamentResolver tournamentResolver) {

    return new GetTournamentStateQueryHandler(tournamentResolver);
  }

}
