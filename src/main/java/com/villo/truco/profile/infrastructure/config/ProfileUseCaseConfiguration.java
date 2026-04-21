package com.villo.truco.profile.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.profile.application.services.ProfileAchievementTrackingService;
import com.villo.truco.profile.domain.model.AchievementPolicy;
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import com.villo.truco.profile.infrastructure.eventhandlers.ProfileMatchDomainEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileUseCaseConfiguration {

  private final BotRegistry botRegistry;
  private final UserQueryRepository userQueryRepository;
  private final MatchAchievementTrackerRepository matchAchievementTrackerRepository;
  private final PlayerProfileRepository playerProfileRepository;
  private final ProfileEventNotifier profileEventNotifier;

  public ProfileUseCaseConfiguration(final BotRegistry botRegistry,
      final UserQueryRepository userQueryRepository,
      final MatchAchievementTrackerRepository matchAchievementTrackerRepository,
      final PlayerProfileRepository playerProfileRepository,
      final ProfileEventNotifier profileEventNotifier) {

    this.botRegistry = botRegistry;
    this.userQueryRepository = userQueryRepository;
    this.matchAchievementTrackerRepository = matchAchievementTrackerRepository;
    this.playerProfileRepository = playerProfileRepository;
    this.profileEventNotifier = profileEventNotifier;
  }

  @Bean
  AchievementPolicy achievementPolicy() {

    return new AchievementPolicy();
  }

  @Bean
  ProfileAchievementTrackingService profileAchievementTrackingService(
      final AchievementPolicy achievementPolicy) {

    return new ProfileAchievementTrackingService(this.botRegistry, this.userQueryRepository,
        this.matchAchievementTrackerRepository, this.playerProfileRepository, achievementPolicy,
        this.profileEventNotifier);
  }

  @Bean
  ProfileMatchDomainEventHandler profileMatchDomainEventHandler(
      final ProfileAchievementTrackingService profileAchievementTrackingService) {

    return new ProfileMatchDomainEventHandler(profileAchievementTrackingService);
  }
}
