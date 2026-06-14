package com.villo.truco.campaign.domain.model;

public final class CampaignProgressRehydrator {

  private CampaignProgressRehydrator() {

  }

  public static CampaignProgress rehydrate(final CampaignProgressSnapshot snapshot) {

    return CampaignProgress.reconstruct(snapshot.playerId(), snapshot.points(),
        snapshot.activeChallenge(), snapshot.rivalRecords(), snapshot.topOneReached(),
        snapshot.allRivalsDefeated(), snapshot.unlockedCasualBots(),
        snapshot.allCasualBotsUnlocked());
  }

}
