package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.AggregateLockManager;
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
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.persistence.InMemoryAggregateLockManager;
import com.villo.truco.infrastructure.persistence.repositories.InMemoryUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

  @Bean
  AggregateLockManager<MatchId> matchLockManager() {

    return new InMemoryAggregateLockManager<>();
  }

  @Bean
  AggregateLockManager<TournamentId> tournamentLockManager() {

    return new InMemoryAggregateLockManager<>();
  }

  @Bean
  UserRepository userRepository() {

    return new InMemoryUserRepository();
  }

  @Bean
  MatchResolver matchResolver(final MatchQueryRepository matchQueryRepository) {

    return new MatchResolver(matchQueryRepository);
  }

  @Bean
  RegisterUserUseCase registerUserCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider) {

    return new RegisterUserCommandHandler(userRepository, passwordHasher, tokenProvider);
  }

  @Bean
  LoginUseCase loginCommandHandler(final UserRepository userRepository,
      final PasswordHasher passwordHasher, final PlayerTokenProvider tokenProvider) {

    return new LoginCommandHandler(userRepository, passwordHasher, tokenProvider);
  }

  @Bean
  GuestLoginUseCase guestLoginCommandHandler(final PlayerTokenProvider tokenProvider) {

    return new GuestLoginCommandHandler(tokenProvider);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler(final MatchRepository matchRepository) {

    return new CreateMatchCommandHandler(matchRepository);
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new JoinMatchCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  StartMatchUseCase startMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchQueryRepository matchQueryRepository,
      final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new StartMatchCommandHandler(matchResolver, matchRepository, matchQueryRepository,
        matchEventNotifier, matchLockManager);
  }

  @Bean
  PlayCardUseCase playCardCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new PlayCardCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  CallTrucoUseCase callTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new CallTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  RespondTrucoUseCase respondTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new RespondTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  CallEnvidoUseCase callEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new CallEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  RespondEnvidoUseCase respondEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new RespondEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  FoldUseCase foldCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final AggregateLockManager<MatchId> matchLockManager) {

    return new FoldCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
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
      final TournamentRepository tournamentRepository) {

    return new CreateTournamentCommandHandler(tournamentRepository);
  }

  @Bean
  JoinTournamentUseCase joinTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    return new JoinTournamentCommandHandler(tournamentResolver, tournamentRepository,
        tournamentLockManager);
  }

  @Bean
  StartTournamentUseCase startTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository, final MatchRepository matchRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    return new StartTournamentCommandHandler(tournamentResolver, tournamentRepository,
        matchRepository, tournamentLockManager);
  }

  @Bean
  LeaveTournamentUseCase leaveTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    return new LeaveTournamentCommandHandler(tournamentResolver, tournamentRepository,
        tournamentLockManager);
  }

  @Bean
  RegisterTournamentMatchResultUseCase registerTournamentMatchResultCommandHandler(
      final TournamentResolver tournamentResolver, final TournamentRepository tournamentRepository,
      final MatchQueryRepository matchQueryRepository) {

    return new RegisterTournamentMatchResultCommandHandler(tournamentResolver, tournamentRepository,
        matchQueryRepository);
  }

  @Bean
  GetTournamentStateUseCase getTournamentStateQueryHandler(
      final TournamentResolver tournamentResolver) {

    return new GetTournamentStateQueryHandler(tournamentResolver);
  }

}
