package com.villo.truco.profile.domain.model;

public final class PlayerProfileRehydrator {

  private PlayerProfileRehydrator() {

  }

  public static PlayerProfile rehydrate(final PlayerProfileSnapshot snapshot) {

    return PlayerProfile.reconstruct(snapshot.playerId(), snapshot.unlockedAchievements());
  }
}
