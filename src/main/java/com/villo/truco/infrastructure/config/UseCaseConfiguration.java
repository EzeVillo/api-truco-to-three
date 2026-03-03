package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.SessionGrantProvider;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.application.ports.in.ExchangeSessionGrantUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RefreshSessionUseCase;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.usecases.commands.CallEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.CallTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.CreateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.CreateTournamentCommandHandler;
import com.villo.truco.application.usecases.commands.ExchangeSessionGrantCommandHandler;
import com.villo.truco.application.usecases.commands.FoldCommandHandler;
import com.villo.truco.application.usecases.commands.JoinMatchCommandHandler;
import com.villo.truco.application.usecases.commands.MatchResolver;
import com.villo.truco.application.usecases.commands.PlayCardCommandHandler;
import com.villo.truco.application.usecases.commands.RefreshSessionCommandHandler;
import com.villo.truco.application.usecases.commands.RegisterTournamentMatchResultCommandHandler;
import com.villo.truco.application.usecases.commands.RespondEnvidoCommandHandler;
import com.villo.truco.application.usecases.commands.RespondTrucoCommandHandler;
import com.villo.truco.application.usecases.commands.TournamentResolver;
import com.villo.truco.application.usecases.queries.GetMatchStateQueryHandler;
import com.villo.truco.application.usecases.queries.GetTournamentStateQueryHandler;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchRulesProperties.class)
public class UseCaseConfiguration {

  @Bean
  MatchRules matchRules(final MatchRulesProperties properties) {

    return new MatchRules(properties.getGamesToWin(), properties.getPointsToWinGame());
  }

  @Bean
  MatchResolver matchResolver(final MatchQueryRepository matchQueryRepository) {

    return new MatchResolver(matchQueryRepository);
  }

  @Bean
  CreateMatchUseCase createMatchCommandHandler(final MatchRepository matchRepository,
      final MatchRules matchRules, final SessionGrantProvider sessionGrantProvider) {

    return new CreateMatchCommandHandler(matchRepository, matchRules, sessionGrantProvider);
  }

  @Bean
  JoinMatchUseCase joinMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final SessionGrantProvider sessionGrantProvider) {

    return new JoinMatchCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        sessionGrantProvider);
  }

  @Bean
  ExchangeSessionGrantUseCase exchangeSessionGrantCommandHandler(
      final SessionGrantProvider sessionGrantProvider, final PlayerTokenProvider tokenProvider) {

    return new ExchangeSessionGrantCommandHandler(sessionGrantProvider, tokenProvider);
  }

  @Bean
  RefreshSessionUseCase refreshSessionCommandHandler(final PlayerTokenProvider tokenProvider) {

    return new RefreshSessionCommandHandler(tokenProvider);
  }

  @Bean
  PlayCardUseCase playCardCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new PlayCardCommandHandler(matchResolver, matchRepository, matchEventNotifier);
  }

  @Bean
  CallTrucoUseCase callTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new CallTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier);
  }

  @Bean
  RespondTrucoUseCase respondTrucoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new RespondTrucoCommandHandler(matchResolver, matchRepository, matchEventNotifier);
  }

  @Bean
  CallEnvidoUseCase callEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new CallEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier);
  }

  @Bean
  RespondEnvidoUseCase respondEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new RespondEnvidoCommandHandler(matchResolver, matchRepository, matchEventNotifier);
  }

  @Bean
  FoldUseCase foldCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    return new FoldCommandHandler(matchResolver, matchRepository, matchEventNotifier);
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
