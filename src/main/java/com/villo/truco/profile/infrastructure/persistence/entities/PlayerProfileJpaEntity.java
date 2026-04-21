package com.villo.truco.profile.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "player_profiles")
public class PlayerProfileJpaEntity {

  @Id
  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "player_achievements", joinColumns = @JoinColumn(name = "player_id"),
      uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "achievement_code"}))
  @OrderBy("unlockedAt ASC")
  private List<UnlockedAchievementJpaEmbeddable> unlockedAchievements = new ArrayList<>();

  @Version
  private int version;

  public UUID getPlayerId() {

    return this.playerId;
  }

  public void setPlayerId(final UUID playerId) {

    this.playerId = playerId;
  }

  public List<UnlockedAchievementJpaEmbeddable> getUnlockedAchievements() {

    return this.unlockedAchievements;
  }

  public void setUnlockedAchievements(
      final List<UnlockedAchievementJpaEmbeddable> unlockedAchievements) {

    this.unlockedAchievements = unlockedAchievements;
  }

  public int getVersion() {

    return this.version;
  }

  public void setVersion(final int version) {

    this.version = version;
  }
}
