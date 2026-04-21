package com.villo.truco.profile.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public class UnlockedAchievementJpaEmbeddable {

  @Column(name = "achievement_code", nullable = false)
  private String achievementCode;

  @Column(name = "unlocked_at", nullable = false)
  private Instant unlockedAt;

  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "game_number", nullable = false)
  private int gameNumber;

  public String getAchievementCode() {

    return this.achievementCode;
  }

  public void setAchievementCode(final String achievementCode) {

    this.achievementCode = achievementCode;
  }

  public Instant getUnlockedAt() {

    return this.unlockedAt;
  }

  public void setUnlockedAt(final Instant unlockedAt) {

    this.unlockedAt = unlockedAt;
  }

  public UUID getMatchId() {

    return this.matchId;
  }

  public void setMatchId(final UUID matchId) {

    this.matchId = matchId;
  }

  public int getGameNumber() {

    return this.gameNumber;
  }

  public void setGameNumber(final int gameNumber) {

    this.gameNumber = gameNumber;
  }
}
