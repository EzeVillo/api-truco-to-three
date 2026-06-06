package com.villo.truco.infrastructure.config;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.eventhandlers.MatchEventMapper;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnCupMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorAutoKickOnLeagueMatchActivatedEventHandler;
import com.villo.truco.application.eventhandlers.SpectatorCleanupOnFriendshipRemovedEventHandler;
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
import com.villo.truco.domain.ports.FriendshipSpectateEligibilityResolver;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
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
  private final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver;
  private final long idleTimeoutMillis;
  private final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private final PresenceNotifier presenceNotifier;

  public SpectatorConfiguration(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final ApplicationEventPublisher eventPublisher,
      final MatchEventMapper matchEventMapper, final PublicActorResolver publicActorResolver,
      final FriendshipSpectateEligibilityResolver friendshipSpectateEligibilityResolver,
      final MatchTimeoutProperties matchTimeoutProperties,
      final FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier,
      final PresenceNotifier presenceNotifier) {

    this.matchQueryRepository = matchQueryRepository;
    this.leagueQueryRepository = leagueQueryRepository;
    this.cupQueryRepository = cupQueryRepository;
    this.eventPublisher = eventPublisher;
    this.matchEventMapper = matchEventMapper;
    this.publicActorResolver = publicActorResolver;
    this.friendshipSpectateEligibilityResolver = friendshipSpectateEligibilityResolver;
    this.idleTimeoutMillis = matchTimeoutProperties.getIdleTimeoutSeconds() * 1000L;
    this.friendAvailabilityChangeNotifier = friendAvailabilityChangeNotifier;
    this.presenceNotifier = presenceNotifier;
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

    return new SpectatingEligibilityPolicy(competitionMembershipResolver(),
        this.friendshipSpectateEligibilityResolver);
  }

  @Bean
  SpectatorCountChangedPublisher spectatorCountChangedPublisher() {

    return new SpectatorCountChangedPublisher(this.matchQueryRepository, spectatorshipRepository(),
        this.eventPublisher);
  }

  @Bean
  SpectatorshipLifecycleManager spectatorshipLifecycleManager() {

    return new SpectatorshipLifecycleManager(spectatorshipRepository(),
        spectatorCountChangedPublisher(), this.friendAvailabilityChangeNotifier,
        this.presenceNotifier);
  }

  @Bean
  SpectatorMatchStateDTOAssembler spectatorMatchStateDTOAssembler() {

    return new SpectatorMatchStateDTOAssembler(this.publicActorResolver, this.idleTimeoutMillis);
  }

  @Bean
  SpectateMatchUseCase spectateMatchUseCase() {

    return new SpectateMatchCommandHandler(this.matchQueryRepository, spectatorshipRepository(),
        spectatingEligibilityPolicy(), spectatorCountChangedPublisher(),
        spectatorMatchStateDTOAssembler(), this.friendAvailabilityChangeNotifier,
        this.presenceNotifier);
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
  SpectatorCleanupOnFriendshipRemovedEventHandler spectatorCleanupOnFriendshipRemovedEventHandler() {

    return new SpectatorCleanupOnFriendshipRemovedEventHandler(spectatorshipRepository(),
        this.matchQueryRepository, competitionMembershipResolver(),
        this.friendshipSpectateEligibilityResolver, spectatorshipLifecycleManager());
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
