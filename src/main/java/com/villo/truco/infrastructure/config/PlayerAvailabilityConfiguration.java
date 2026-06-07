package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class PlayerAvailabilityConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final BotRegistry botRegistry;
  private final RematchSessionRepository rematchSessionRepository;
  private final QuickMatchQueuePort quickMatchQueuePort;
  private final SpectatorshipRepository spectatorshipRepository;

  public PlayerAvailabilityConfiguration(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final BotRegistry botRegistry,
      final RematchSessionRepository rematchSessionRepository,
      final QuickMatchQueuePort quickMatchQueuePort,
      @Lazy final SpectatorshipRepository spectatorshipRepository) {

    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.botRegistry = botRegistry;
    this.rematchSessionRepository = rematchSessionRepository;
    this.quickMatchQueuePort = quickMatchQueuePort;
    this.spectatorshipRepository = spectatorshipRepository;
  }

  @Bean
  PlayerAvailabilityChecker playerAvailabilityChecker() {

    return new PlayerAvailabilityChecker(this.matchQueryRepository, this.leagueQueryRepository,
        this.cupQueryRepository, this.botRegistry, this.rematchSessionRepository,
        this.quickMatchQueuePort, this.spectatorshipRepository);
  }

}
