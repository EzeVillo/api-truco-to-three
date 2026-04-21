package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AchievementUnlockDecision(PlayerId playerId, AchievementCode achievementCode) {

  public AchievementUnlockDecision {

    Objects.requireNonNull(playerId, "playerId cannot be null");
    Objects.requireNonNull(achievementCode, "achievementCode cannot be null");
  }
}
