package com.villo.truco.infrastructure.config;

import com.villo.truco.application.assemblers.PublicCupLobbyDTOAssembler;
import com.villo.truco.application.assemblers.PublicLeagueLobbyDTOAssembler;
import com.villo.truco.application.assemblers.PublicMatchLobbyDTOAssembler;
import com.villo.truco.application.eventhandlers.PublicCupLobbyEventTranslator;
import com.villo.truco.application.eventhandlers.PublicLeagueLobbyEventTranslator;
import com.villo.truco.application.eventhandlers.PublicMatchLobbyEventTranslator;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicLobbyConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final PublicActorResolver publicActorResolver;
  private final ApplicationEventPublisher applicationEventPublisher;

  public PublicLobbyConfiguration(final MatchQueryRepository matchQueryRepository,
      final CupQueryRepository cupQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final PublicActorResolver publicActorResolver,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.matchQueryRepository = matchQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.publicActorResolver = publicActorResolver;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Bean
  PublicMatchLobbyEventTranslator publicMatchLobbyEventTranslator() {

    return new PublicMatchLobbyEventTranslator(this.matchQueryRepository,
        new PublicMatchLobbyDTOAssembler(this.publicActorResolver), this.applicationEventPublisher);
  }

  @Bean
  PublicCupLobbyEventTranslator publicCupLobbyEventTranslator() {

    return new PublicCupLobbyEventTranslator(this.cupQueryRepository,
        new PublicCupLobbyDTOAssembler(this.publicActorResolver), this.applicationEventPublisher);
  }

  @Bean
  PublicLeagueLobbyEventTranslator publicLeagueLobbyEventTranslator() {

    return new PublicLeagueLobbyEventTranslator(this.leagueQueryRepository,
        new PublicLeagueLobbyDTOAssembler(this.publicActorResolver),
        this.applicationEventPublisher);
  }

}
