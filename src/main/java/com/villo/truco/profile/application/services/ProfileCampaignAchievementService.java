package com.villo.truco.profile.application.services;

import com.villo.truco.campaign.domain.model.events.AllCampaignBotsUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignAllRivalsDefeatedEvent;
import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.campaign.domain.model.events.CampaignTopOneReachedEvent;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import java.time.Instant;
import java.util.Objects;

public final class ProfileCampaignAchievementService {

  private final PlayerProfileRepository playerProfileRepository;
  private final ProfileEventNotifier profileEventNotifier;

  public ProfileCampaignAchievementService(final PlayerProfileRepository playerProfileRepository,
      final ProfileEventNotifier profileEventNotifier) {

    this.playerProfileRepository = Objects.requireNonNull(playerProfileRepository);
    this.profileEventNotifier = Objects.requireNonNull(profileEventNotifier);
  }

  public void handle(final CampaignDomainEvent event) {

    switch (event) {
      case CampaignTopOneReachedEvent e ->
          this.unlock(e, AchievementCode.REACH_CAMPAIGN_TOP_ONE, e.getMatchId(), e.getGameNumber());
      case CampaignAllRivalsDefeatedEvent e ->
          this.unlock(e, AchievementCode.DEFEAT_ALL_CAMPAIGN_RIVALS, e.getMatchId(),
              e.getGameNumber());
      case AllCampaignBotsUnlockedForCasualEvent e ->
          this.unlock(e, AchievementCode.UNLOCK_ALL_CAMPAIGN_BOTS_IN_CASUAL, e.getMatchId(),
              e.getGameNumber());
      default -> {
      }
    }
  }

  private void unlock(final CampaignDomainEvent event, final AchievementCode achievementCode,
      final MatchId matchId, final int gameNumber) {

    final var profile = this.playerProfileRepository.findByPlayerId(event.getPlayerId())
        .orElseGet(() -> PlayerProfile.create(event.getPlayerId()));

    final var unlocked = profile.unlock(achievementCode, Instant.ofEpochMilli(event.getTimestamp()),
        matchId, gameNumber);
    if (!unlocked) {
      return;
    }

    this.playerProfileRepository.save(profile);
    this.profileEventNotifier.publishDomainEvents(profile.getAchievementUnlockedEvents());
    profile.clearDomainEvents();
  }

}
