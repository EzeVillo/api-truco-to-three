package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.MatchLockManager;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CreateTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterTournamentMatchResultCommandHandler;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.StartMatchCommandHandler;
import com.villo.truco.application.usecases.commands.TournamentResolver;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetTournamentStateQueryHandler;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

  @Bean
  MatchRules matchRules() {

    return MatchRules.fromGamesToPlay(5);
  }

  @Bean
  MatchResolver matchResolver(final MatchQueryRepository matchQueryRepository) {

    return new MatchResolver(matchQueryRepository);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler(final MatchRepository matchRepository,
      final PlayerTokenProvider tokenProvider) {

    return new CreateMatchCommandHandler(matchRepository, tokenProvider);
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final PlayerTokenProvider tokenProvider, final MatchLockManager matchLockManager) {

    return new JoinMatchCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        tokenProvider, matchLockManager);
  }

  @Bean
  StartMatchUseCase startMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new StartMatchCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  PlayCardUseCase playCardCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new PlayCardCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  CallTrucoUseCase callTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new CallTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  RespondTrucoUseCase respondTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new RespondTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  CallEnvidoUseCase callEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new CallEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  RespondEnvidoUseCase respondEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

    return new RespondEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);
  }

  @Bean
  FoldUseCase foldCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final MatchLockManager matchLockManager) {

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
      final TournamentRepository tournamentRepository, final MatchRepository matchRepository,
      final MatchRules matchRules) {

    return new CreateTournamentCommandHandler(tournamentRepository, matchRepository, matchRules);
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
