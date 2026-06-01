package com.villo.truco.profile.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import com.villo.truco.profile.application.eventhandlers.ProfileUserRegisteredEventHandler;
import com.villo.truco.profile.application.services.ProfileAchievementTrackingService;
import com.villo.truco.profile.application.services.ProfilePlayerStatsTrackingService;
import com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogQueryHandler;
import com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogUseCase;
import com.villo.truco.profile.application.usecases.queries.GetPlayerProfileQueryHandler;
import com.villo.truco.profile.application.usecases.queries.GetPlayerProfileUseCase;
import com.villo.truco.profile.domain.model.AchievementPolicy;
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import com.villo.truco.profile.domain.ports.ProcessedMatchStatsRegistry;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import com.villo.truco.profile.infrastructure.eventhandlers.ProfileMatchDomainEventHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileUseCaseConfiguration {

  private final UserQueryRepository userQueryRepository;
  private final MatchAchievementTrackerRepository matchAchievementTrackerRepository;
  private final PlayerProfileRepository playerProfileRepository;
  private final ProfileEventNotifier profileEventNotifier;
  private final PlayerStatsRepository playerStatsRepository;
  private final ProcessedMatchStatsRegistry processedMatchStatsRegistry;
  private final BotRegistry botRegistry;
  private final UseCasePipeline transactionalPipeline;

  public ProfileUseCaseConfiguration(final UserQueryRepository userQueryRepository,
      final MatchAchievementTrackerRepository matchAchievementTrackerRepository,
      final PlayerProfileRepository playerProfileRepository,
      final ProfileEventNotifier profileEventNotifier,
      final PlayerStatsRepository playerStatsRepository,
      final ProcessedMatchStatsRegistry processedMatchStatsRegistry, final BotRegistry botRegistry,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline) {

    this.userQueryRepository = userQueryRepository;
    this.matchAchievementTrackerRepository = matchAchievementTrackerRepository;
    this.playerProfileRepository = playerProfileRepository;
    this.profileEventNotifier = profileEventNotifier;
    this.playerStatsRepository = playerStatsRepository;
    this.processedMatchStatsRegistry = processedMatchStatsRegistry;
    this.botRegistry = botRegistry;
    this.transactionalPipeline = transactionalPipeline;
  }

  @Bean
  AchievementPolicy achievementPolicy() {

    return new AchievementPolicy();
  }

  @Bean
  ProfileAchievementTrackingService profileAchievementTrackingService(
      final AchievementPolicy achievementPolicy) {

    return new ProfileAchievementTrackingService(this.userQueryRepository,
        this.matchAchievementTrackerRepository, this.playerProfileRepository, achievementPolicy,
        this.profileEventNotifier);
  }

  @Bean
  ProfilePlayerStatsTrackingService profilePlayerStatsTrackingService() {

    return new ProfilePlayerStatsTrackingService(this.botRegistry, this.userQueryRepository,
        this.playerStatsRepository, this.processedMatchStatsRegistry);
  }

  @Bean
  ProfileMatchDomainEventHandler profileMatchDomainEventHandler(
      final ProfileAchievementTrackingService profileAchievementTrackingService,
      final ProfilePlayerStatsTrackingService profilePlayerStatsTrackingService) {

    return new ProfileMatchDomainEventHandler(profileAchievementTrackingService,
        profilePlayerStatsTrackingService);
  }

  @Bean
  ProfileUserRegisteredEventHandler profileUserRegisteredEventHandler() {

    return new ProfileUserRegisteredEventHandler(this.playerProfileRepository,
        this.playerStatsRepository);
  }

  @Bean
  GetPlayerProfileUseCase getPlayerProfileUseCase() {

    final var handler = new GetPlayerProfileQueryHandler(this.userQueryRepository,
        this.playerProfileRepository, this.playerStatsRepository);
    return this.transactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetAchievementCatalogUseCase getAchievementCatalogUseCase() {

    return new GetAchievementCatalogQueryHandler();
  }

}
