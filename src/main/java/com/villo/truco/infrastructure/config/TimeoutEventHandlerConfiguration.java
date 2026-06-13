package com.villo.truco.infrastructure.config;

import com.villo.truco.application.eventhandlers.CupTimeoutEventHandler;
import com.villo.truco.application.eventhandlers.LeagueTimeoutEventHandler;
import com.villo.truco.application.eventhandlers.MatchTimeoutEventHandler;
import com.villo.truco.application.eventhandlers.RematchSessionTimeoutEventHandler;
import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.in.ExpireRematchSessionUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleLeaguesUseCase;
import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.application.ports.out.ResourceInvitationDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.application.timeout.CupTimeoutPhasePolicy;
import com.villo.truco.application.timeout.LeagueTimeoutPhasePolicy;
import com.villo.truco.application.timeout.MatchTimeoutPhasePolicy;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.infrastructure.scheduler.CupTimeoutReconciliationSource;
import com.villo.truco.infrastructure.scheduler.LeagueTimeoutReconciliationSource;
import com.villo.truco.infrastructure.scheduler.MatchTimeoutReconciliationSource;
import com.villo.truco.infrastructure.scheduler.RematchSessionTimeoutReconciliationSource;
import com.villo.truco.social.application.eventhandlers.ResourceInvitationTimeoutEventHandler;
import com.villo.truco.social.application.ports.in.ExpireResourceInvitationUseCase;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.infrastructure.scheduler.ResourceInvitationTimeoutReconciliationSource;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class TimeoutEventHandlerConfiguration {

  private final TimeoutIdleMatchesUseCase timeoutIdleMatchesUseCase;
  private final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase;
  private final TimeoutIdleLeaguesUseCase timeoutIdleLeaguesUseCase;
  private final ExpireRematchSessionUseCase expireRematchSessionUseCase;
  private final ExpireResourceInvitationUseCase expireResourceInvitationUseCase;
  private final MatchRepository matchRepository;
  private final CupRepository cupRepository;
  private final LeagueRepository leagueRepository;
  private final RematchSessionRepository rematchSessionRepository;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final MatchTimeoutProperties matchTimeoutProperties;
  private final CupTimeoutProperties cupTimeoutProperties;
  private final LeagueTimeoutProperties leagueTimeoutProperties;
  private final MatchTimeoutPhasePolicy matchTimeoutPhasePolicy = new MatchTimeoutPhasePolicy();
  private final LeagueTimeoutPhasePolicy leagueTimeoutPhasePolicy = new LeagueTimeoutPhasePolicy();
  private final CupTimeoutPhasePolicy cupTimeoutPhasePolicy = new CupTimeoutPhasePolicy();

  public TimeoutEventHandlerConfiguration(
      @Lazy final TimeoutIdleMatchesUseCase timeoutIdleMatchesUseCase,
      @Lazy final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase,
      @Lazy final TimeoutIdleLeaguesUseCase timeoutIdleLeaguesUseCase,
      @Lazy final ExpireRematchSessionUseCase expireRematchSessionUseCase,
      @Lazy final ExpireResourceInvitationUseCase expireResourceInvitationUseCase,
      final MatchRepository matchRepository, final CupRepository cupRepository,
      final LeagueRepository leagueRepository,
      final RematchSessionRepository rematchSessionRepository,
      final ResourceInvitationRepository resourceInvitationRepository,
      final MatchTimeoutProperties matchTimeoutProperties,
      final CupTimeoutProperties cupTimeoutProperties,
      final LeagueTimeoutProperties leagueTimeoutProperties) {

    this.timeoutIdleMatchesUseCase = timeoutIdleMatchesUseCase;
    this.timeoutIdleCupsUseCase = timeoutIdleCupsUseCase;
    this.timeoutIdleLeaguesUseCase = timeoutIdleLeaguesUseCase;
    this.expireRematchSessionUseCase = expireRematchSessionUseCase;
    this.expireResourceInvitationUseCase = expireResourceInvitationUseCase;
    this.matchRepository = matchRepository;
    this.cupRepository = cupRepository;
    this.leagueRepository = leagueRepository;
    this.rematchSessionRepository = rematchSessionRepository;
    this.resourceInvitationRepository = resourceInvitationRepository;
    this.matchTimeoutProperties = matchTimeoutProperties;
    this.cupTimeoutProperties = cupTimeoutProperties;
    this.leagueTimeoutProperties = leagueTimeoutProperties;
  }

  @Bean
  TimeoutActionDispatcher timeoutActionDispatcher() {

    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.MATCH,
        id -> () -> this.timeoutIdleMatchesUseCase.handle(new MatchId(UUID.fromString(id))));
    dispatcher.register(EntityType.CUP,
        id -> () -> this.timeoutIdleCupsUseCase.handle(new CupId(UUID.fromString(id))));
    dispatcher.register(EntityType.LEAGUE,
        id -> () -> this.timeoutIdleLeaguesUseCase.handle(new LeagueId(UUID.fromString(id))));
    dispatcher.register(EntityType.REMATCH_SESSION,
        id -> () -> this.expireRematchSessionUseCase.handle(
            new RematchSessionId(UUID.fromString(id))));
    dispatcher.register(EntityType.RESOURCE_INVITATION,
        id -> () -> this.expireResourceInvitationUseCase.handle(ResourceInvitationId.of(id)));
    return dispatcher;
  }

  @Bean
  MatchDomainEventHandler<MatchDomainEvent> matchTimeoutEventHandler(
      final TimeoutScheduler timeoutScheduler) {

    return new MatchTimeoutEventHandler(timeoutScheduler, timeoutActionDispatcher(),
        this.matchTimeoutPhasePolicy, matchLobbyTimeout(), matchPlayTimeout());
  }

  @Bean
  CupDomainEventHandler<CupDomainEvent> cupTimeoutEventHandler(
      final TimeoutScheduler timeoutScheduler) {

    return new CupTimeoutEventHandler(timeoutScheduler, timeoutActionDispatcher(),
        this.cupTimeoutPhasePolicy, cupLobbyTimeout());
  }

  @Bean
  LeagueDomainEventHandler<LeagueDomainEvent> leagueTimeoutEventHandler(
      final TimeoutScheduler timeoutScheduler) {

    return new LeagueTimeoutEventHandler(timeoutScheduler, timeoutActionDispatcher(),
        this.leagueTimeoutPhasePolicy, leagueLobbyTimeout());
  }

  @Bean
  TimeoutReconciliationSource matchTimeoutReconciliationSource() {

    return new MatchTimeoutReconciliationSource(this.matchRepository, timeoutActionDispatcher(),
        this.matchTimeoutPhasePolicy, matchLobbyTimeout(), matchPlayTimeout());
  }

  @Bean
  TimeoutReconciliationSource cupTimeoutReconciliationSource() {

    return new CupTimeoutReconciliationSource(this.cupRepository, timeoutActionDispatcher(),
        this.cupTimeoutPhasePolicy, cupLobbyTimeout());
  }

  @Bean
  TimeoutReconciliationSource leagueTimeoutReconciliationSource() {

    return new LeagueTimeoutReconciliationSource(this.leagueRepository, timeoutActionDispatcher(),
        this.leagueTimeoutPhasePolicy, leagueLobbyTimeout());
  }

  private Duration matchLobbyTimeout() {

    return Duration.ofSeconds(this.matchTimeoutProperties.getLobbyTimeoutSeconds());
  }

  private Duration matchPlayTimeout() {

    return Duration.ofSeconds(this.matchTimeoutProperties.getPlayTimeoutSeconds());
  }

  private Duration leagueLobbyTimeout() {

    return Duration.ofSeconds(this.leagueTimeoutProperties.getLobbyTimeoutSeconds());
  }

  private Duration cupLobbyTimeout() {

    return Duration.ofSeconds(this.cupTimeoutProperties.getLobbyTimeoutSeconds());
  }

  @Bean
  RematchSessionDomainEventHandler<RematchSessionDomainEvent> rematchSessionTimeoutEventHandler(
      final TimeoutScheduler timeoutScheduler) {

    return new RematchSessionTimeoutEventHandler(timeoutScheduler, timeoutActionDispatcher());
  }

  @Bean
  ResourceInvitationDomainEventHandler<ResourceInvitationDomainEvent> resourceInvitationTimeoutEventHandler(
      final TimeoutScheduler timeoutScheduler) {

    return new ResourceInvitationTimeoutEventHandler(timeoutScheduler, timeoutActionDispatcher());
  }

  @Bean
  TimeoutReconciliationSource rematchSessionTimeoutReconciliationSource() {

    return new RematchSessionTimeoutReconciliationSource(this.rematchSessionRepository,
        timeoutActionDispatcher());
  }

  @Bean
  TimeoutReconciliationSource resourceInvitationTimeoutReconciliationSource() {

    return new ResourceInvitationTimeoutReconciliationSource(this.resourceInvitationRepository,
        timeoutActionDispatcher());
  }

}
