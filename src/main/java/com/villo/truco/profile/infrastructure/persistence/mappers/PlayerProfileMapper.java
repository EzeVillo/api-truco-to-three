package com.villo.truco.profile.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.model.PlayerProfileRehydrator;
import com.villo.truco.profile.domain.model.PlayerProfileSnapshot;
import com.villo.truco.profile.domain.model.UnlockedAchievement;
import com.villo.truco.profile.infrastructure.persistence.entities.PlayerProfileJpaEntity;
import com.villo.truco.profile.infrastructure.persistence.entities.UnlockedAchievementJpaEmbeddable;
import org.springframework.stereotype.Component;

@Component
public class PlayerProfileMapper {

  public PlayerProfileJpaEntity toEntity(final PlayerProfile profile) {

    final var snapshot = profile.snapshot();
    final var entity = new PlayerProfileJpaEntity();
    entity.setPlayerId(snapshot.playerId().value());
    entity.setUnlockedAchievements(
        snapshot.unlockedAchievements().stream().map(this::toEmbeddable).toList());
    entity.setVersion((int) profile.getVersion());
    return entity;
  }

  public PlayerProfile toDomain(final PlayerProfileJpaEntity entity) {

    final var snapshot = new PlayerProfileSnapshot(new PlayerId(entity.getPlayerId()),
        entity.getUnlockedAchievements().stream().map(this::toValueObject).toList());
    final var profile = PlayerProfileRehydrator.rehydrate(snapshot);
    profile.setVersion(entity.getVersion());
    return profile;
  }

  private UnlockedAchievementJpaEmbeddable toEmbeddable(
      final UnlockedAchievement unlockedAchievement) {

    final var embeddable = new UnlockedAchievementJpaEmbeddable();
    embeddable.setAchievementCode(unlockedAchievement.achievementCode().name());
    embeddable.setUnlockedAt(unlockedAchievement.unlockedAt());
    embeddable.setMatchId(unlockedAchievement.matchId().value());
    embeddable.setGameNumber(unlockedAchievement.gameNumber());
    return embeddable;
  }

  private UnlockedAchievement toValueObject(
      final UnlockedAchievementJpaEmbeddable unlockedAchievementJpaEmbeddable) {

    return new UnlockedAchievement(
        AchievementCode.valueOf(unlockedAchievementJpaEmbeddable.getAchievementCode()),
        unlockedAchievementJpaEmbeddable.getUnlockedAt(),
        new MatchId(unlockedAchievementJpaEmbeddable.getMatchId()),
        unlockedAchievementJpaEmbeddable.getGameNumber());
  }
}
