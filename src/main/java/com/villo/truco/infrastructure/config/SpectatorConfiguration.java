package com.villo.truco.infrastructure.config;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.eventhandlers.MatchEventMapper;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnCupMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnLeagueMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorCleanupOnMatchEndEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorNotificationEventTranslator;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetSpectateMatchStateUseCase;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import com.villo.truco.application.ports.in.StopSpectatingMatchUseCase;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.commands.SpectateMatchCommandHandler;
import com.villo.truco.application.usecases.commands.SpectatorCountChangedPublisher;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.application.usecases.commands.StopSpectatingMatchCommandHandler;
import com.villo.truco.application.usecases.queries.GetSpectateMatchStateQueryHandler;
import com.villo.truco.domain.model.spectator.SpectatingEligibilityPolicy;
import com.villo.truco.domain.ports.CompetitionMembershipResolver;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpectatorConfiguration {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final MatchEventMapper matchEventMapper;
  private final PublicActorResolver publicActorResolver;

  public SpectatorConfiguration(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final ApplicationEventPublisher eventPublisher,
      final MatchEventMapper matchEventMapper, final PublicActorResolver publicActorResolver) {

    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.eventPublisher = eventPublisher;
    this.matchEventMapper = matchEventMapper;
    this.publicActorResolver = publicActorResolver;
  }

  @Bean
  SpectatorshipRepository spectatorshipRepository() {

    return new InMemorySpectatorshipRepository();
  }

  @Bean
  CompetitionMembershipResolver competitionMembershipResolver() {

    return (matchId, playerId) -> this.leagueQueryRepository.findByMatchId(matchId)
        .map(league -> league.getParticipants().contains(playerId))
        .or(() -> this.cupQueryRepository.findByMatchId(matchId)
            .map(cup -> cup.getParticipants().contains(playerId))).orElse(false);
  }

  @Bean
  SpectatingEligibilityPolicy spectatingEligibilityPolicy() {

    return new SpectatingEligibilityPolicy(competitionMembershipResolver());
  }

  @Bean
  SpectatorCountChangedPublisher spectatorCountChangedPublisher() {

    return new SpectatorCountChangedPublisher(this.matchQueryRepository, spectatorshipRepository(),
        this.eventPublisher);
  }

  @Bean
  SpectatorshipLifecycleManager spectatorshipLifecycleManager() {

    return new SpectatorshipLifecycleManager(spectatorshipRepository(),
        spectatorCountChangedPublisher());
  }

  @Bean
  SpectatorMatchStateDTOAssembler spectatorMatchStateDTOAssembler() {

    return new SpectatorMatchStateDTOAssembler(this.publicActorResolver);
  }

  @Bean
  SpectateMatchUseCase spectateMatchUseCase() {

    return new SpectateMatchCommandHandler(this.matchQueryRepository, spectatorshipRepository(),
        spectatingEligibilityPolicy(), spectatorCountChangedPublisher(),
        spectatorMatchStateDTOAssembler());
  }

  @Bean
  StopSpectatingMatchUseCase stopSpectatingMatchUseCase() {

    return new StopSpectatingMatchCommandHandler(spectatorshipLifecycleManager());
  }

  @Bean
  GetSpectateMatchStateUseCase getSpectateMatchStateUseCase() {

    return new GetSpectateMatchStateQueryHandler(this.matchQueryRepository,
        spectatorshipRepository(), spectatorMatchStateDTOAssembler());
  }

  @Bean
  SpectatorNotificationEventTranslator spectatorNotificationEventTranslator() {

    return new SpectatorNotificationEventTranslator(spectatorshipRepository(),
        this.matchEventMapper, this.eventPublisher);
  }

  @Bean
  SpectatorCleanupOnMatchEndEventHandler spectatorCleanupOnMatchEndEventHandler() {

    return new SpectatorCleanupOnMatchEndEventHandler(spectatorshipLifecycleManager());
  }

  @Bean
  SpectatorAutoKickOnLeagueMatchActivatedEventHandler spectatorAutoKickOnLeagueMatchActivatedEventHandler() {

    return new SpectatorAutoKickOnLeagueMatchActivatedEventHandler(spectatorshipLifecycleManager());
  }

  @Bean
  SpectatorAutoKickOnCupMatchActivatedEventHandler spectatorAutoKickOnCupMatchActivatedEventHandler() {

    return new SpectatorAutoKickOnCupMatchActivatedEventHandler(spectatorshipLifecycleManager());
  }

}
