package com.villo.truco.profile.application.eventhandlers;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.profile.application.events.ProfileEventNotification;
import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import java.util.Map;
import java.util.Objects;

public final class ProfileNotificationEventTranslator implements
    DomainEventHandler<AchievementUnlocked> {

  private final ApplicationEventPublisher applicationEventPublisher;

  public ProfileNotificationEventTranslator(
      final ApplicationEventPublisher applicationEventPublisher) {

    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  @Override
  public Class<AchievementUnlocked> eventType() {

    return AchievementUnlocked.class;
  }

  @Override
  public void handle(final AchievementUnlocked event) {

    final var payload = Map.<String, Object>of("achievementCode", event.getAchievementCode().name(),
        "unlockedAt", event.getUnlockedAt().toEpochMilli(), "matchId",
        event.getMatchId().value().toString(), "gameNumber", event.getGameNumber());
    this.applicationEventPublisher.publish(
        new ProfileEventNotification(java.util.List.of(event.getPlayerId()), event.getEventType(),
            event.getTimestamp(), payload));
  }
}
