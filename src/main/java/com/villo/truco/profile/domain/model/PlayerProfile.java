package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class PlayerProfile extends AggregateBase<PlayerId> {

  private final List<UnlockedAchievement> unlockedAchievements = new ArrayList<>();
  private final EnumSet<AchievementCode> unlockedCodes = EnumSet.noneOf(AchievementCode.class);

  private PlayerProfile(final PlayerId playerId) {

    super(Objects.requireNonNull(playerId));
  }

  public static PlayerProfile create(final PlayerId playerId) {

    return new PlayerProfile(playerId);
  }

  static PlayerProfile reconstruct(final PlayerId playerId,
      final List<UnlockedAchievement> unlockedAchievements) {

    final var profile = new PlayerProfile(playerId);
    for (final var unlockedAchievement : unlockedAchievements) {
      profile.unlockedAchievements.add(unlockedAchievement);
      profile.unlockedCodes.add(unlockedAchievement.achievementCode());
    }
    return profile;
  }

  public boolean unlock(final AchievementCode achievementCode, final Instant unlockedAt,
      final MatchId matchId, final int gameNumber) {

    Objects.requireNonNull(achievementCode, "achievementCode cannot be null");
    Objects.requireNonNull(unlockedAt, "unlockedAt cannot be null");
    Objects.requireNonNull(matchId, "matchId cannot be null");

    if (!this.unlockedCodes.add(achievementCode)) {
      return false;
    }

    final var unlockedAchievement = new UnlockedAchievement(achievementCode, unlockedAt, matchId,
        gameNumber);
    this.unlockedAchievements.add(unlockedAchievement);
    this.addDomainEvent(
        new AchievementUnlocked(this.id, achievementCode, unlockedAt, matchId, gameNumber));
    return true;
  }

  public boolean hasUnlocked(final AchievementCode achievementCode) {

    return this.unlockedCodes.contains(achievementCode);
  }

  public List<UnlockedAchievement> getUnlockedAchievements() {

    return List.copyOf(this.unlockedAchievements);
  }

  public List<AchievementUnlocked> getAchievementUnlockedEvents() {

    return this.getDomainEvents().stream().map(AchievementUnlocked.class::cast).toList();
  }

  public PlayerProfileSnapshot snapshot() {

    return new PlayerProfileSnapshot(this.id, List.copyOf(this.unlockedAchievements));
  }
}
