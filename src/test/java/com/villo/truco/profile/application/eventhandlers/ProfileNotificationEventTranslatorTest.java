package com.villo.truco.profile.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.application.events.ProfileEventNotification;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileNotificationEventTranslator")
class ProfileNotificationEventTranslatorTest {

  @Test
  @DisplayName("traduce AchievementUnlocked a ProfileEventNotification")
  void translatesAchievementUnlockedToProfileEventNotification() {

    final var published = new ArrayList<ApplicationEvent>();
    final ApplicationEventPublisher publisher = published::add;
    final var translator = new ProfileNotificationEventTranslator(publisher);
    final var playerId = PlayerId.generate();
    final var matchId = MatchId.generate();
    final var unlockedAt = Instant.parse("2026-04-20T21:30:00Z");

    translator.handle(
        new AchievementUnlocked(playerId, AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3, unlockedAt,
            matchId, 1));

    assertThat(published).hasSize(1);
    final var event = (ProfileEventNotification) published.getFirst();
    assertThat(event.recipients()).isEqualTo(List.of(playerId));
    assertThat(event.eventType()).isEqualTo("ACHIEVEMENT_UNLOCKED");
    assertThat(event.payload()).containsEntry("achievementCode", "WIN_RETRUCO_FROM_0_0_TO_3")
        .containsEntry("unlockedAt", unlockedAt.toEpochMilli())
        .containsEntry("matchId", matchId.value().toString())
        .containsEntry("gameNumber", 1);
  }
}
