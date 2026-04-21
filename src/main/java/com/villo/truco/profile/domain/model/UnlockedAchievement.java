package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Instant;
import java.util.Objects;

public record UnlockedAchievement(AchievementCode achievementCode, Instant unlockedAt,
                                  MatchId matchId, int gameNumber) {

  public UnlockedAchievement {

    Objects.requireNonNull(achievementCode, "achievementCode cannot be null");
    Objects.requireNonNull(unlockedAt, "unlockedAt cannot be null");
    Objects.requireNonNull(matchId, "matchId cannot be null");
    if (gameNumber <= 0) {
      throw new IllegalArgumentException("gameNumber must be positive");
    }
  }
}
