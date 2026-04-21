package com.villo.truco.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerProfile")
class PlayerProfileTest {

  @Test
  @DisplayName("unlock es idempotente para el mismo AchievementCode")
  void unlockIsIdempotentForSameAchievementCode() {

    final var profile = PlayerProfile.create(PlayerId.generate());
    final var unlockedAt = Instant.parse("2026-04-20T21:00:00Z");
    final var matchId = MatchId.generate();

    final var firstUnlock = profile.unlock(AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3, unlockedAt,
        matchId, 1);
    final var secondUnlock = profile.unlock(AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3,
        unlockedAt.plusSeconds(1), matchId, 2);

    assertThat(firstUnlock).isTrue();
    assertThat(secondUnlock).isFalse();
    assertThat(profile.getUnlockedAchievements()).hasSize(1);
    assertThat(profile.getAchievementUnlockedEvents()).hasSize(1);
  }
}
