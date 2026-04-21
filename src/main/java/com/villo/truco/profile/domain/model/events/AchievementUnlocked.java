package com.villo.truco.profile.domain.model.events;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.AchievementCode;
import java.time.Instant;
import java.util.Objects;

public final class AchievementUnlocked extends DomainEventBase {

  private final PlayerId playerId;
  private final AchievementCode achievementCode;
  private final Instant unlockedAt;
  private final MatchId matchId;
  private final int gameNumber;

  public AchievementUnlocked(final PlayerId playerId, final AchievementCode achievementCode,
      final Instant unlockedAt, final MatchId matchId, final int gameNumber) {

    super("ACHIEVEMENT_UNLOCKED");
    this.playerId = Objects.requireNonNull(playerId);
    this.achievementCode = Objects.requireNonNull(achievementCode);
    this.unlockedAt = Objects.requireNonNull(unlockedAt);
    this.matchId = Objects.requireNonNull(matchId);
    if (gameNumber <= 0) {
      throw new IllegalArgumentException("gameNumber must be positive");
    }
    this.gameNumber = gameNumber;
  }

  public PlayerId getPlayerId() {

    return this.playerId;
  }

  public AchievementCode getAchievementCode() {

    return this.achievementCode;
  }

  public Instant getUnlockedAt() {

    return this.unlockedAt;
  }

  public MatchId getMatchId() {

    return this.matchId;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }
}
