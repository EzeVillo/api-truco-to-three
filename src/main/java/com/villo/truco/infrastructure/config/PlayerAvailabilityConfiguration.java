package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlayerAvailabilityConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final BotRegistry botRegistry;

  public PlayerAvailabilityConfiguration(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final BotRegistry botRegistry) {

    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.botRegistry = botRegistry;
  }

  @Bean
  PlayerAvailabilityChecker playerAvailabilityChecker() {

    return new PlayerAvailabilityChecker(this.matchQueryRepository, this.leagueQueryRepository,
        this.cupQueryRepository, this.botRegistry);
  }

}
