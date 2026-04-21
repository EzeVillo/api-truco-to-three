package com.villo.truco.profile.domain.model;

public final class MatchAchievementTrackerRehydrator {

  private MatchAchievementTrackerRehydrator() {

  }

  public static MatchAchievementTracker rehydrate(final MatchAchievementTrackerSnapshot snapshot) {

    return MatchAchievementTracker.reconstruct(snapshot);
  }
}
