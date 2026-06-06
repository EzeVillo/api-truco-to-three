package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.in.GetUserPresenceUseCase;
import com.villo.truco.application.usecases.queries.GetUserPresenceQueryHandler;
import com.villo.truco.application.usecases.queries.UserPresenceResolver;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PresenceUseCaseConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final RematchSessionRepository rematchSessionRepository;
  private final QuickMatchQueuePort quickMatchQueuePort;
  private final SpectatorshipRepository spectatorshipRepository;

  public PresenceUseCaseConfiguration(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository,
      final RematchSessionRepository rematchSessionRepository,
      final QuickMatchQueuePort quickMatchQueuePort,
      final SpectatorshipRepository spectatorshipRepository) {

    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.rematchSessionRepository = rematchSessionRepository;
    this.quickMatchQueuePort = quickMatchQueuePort;
    this.spectatorshipRepository = spectatorshipRepository;
  }

  @Bean
  UserPresenceResolver userPresenceResolver() {

    return new UserPresenceResolver(this.matchQueryRepository, this.leagueQueryRepository,
        this.cupQueryRepository, this.rematchSessionRepository, this.quickMatchQueuePort,
        this.spectatorshipRepository);
  }

  @Bean
  GetUserPresenceUseCase getUserPresenceQueryHandler(
      final UserPresenceResolver userPresenceResolver) {

    return new GetUserPresenceQueryHandler(userPresenceResolver);
  }

}
