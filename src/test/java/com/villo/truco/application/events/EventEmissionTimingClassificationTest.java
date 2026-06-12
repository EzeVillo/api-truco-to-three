package com.villo.truco.application.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.campaign.application.events.CampaignEventNotification;
import com.villo.truco.profile.application.events.ProfileEventNotification;
import com.villo.truco.social.application.events.SocialEventNotification;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Clasificacion de timing de emision de los application events")
class EventEmissionTimingClassificationTest {

  private static final List<Class<? extends ApplicationEvent>> USER_NOTIFICATION_EVENTS = List.of(
      MatchEventNotification.class, LeagueEventNotification.class, CupEventNotification.class,
      ChatEventNotification.class, SpectatorMatchEventNotification.class,
      SpectatorCountChanged.class, PresenceEventNotification.class,
      PublicMatchLobbyNotification.class, PublicCupLobbyNotification.class,
      PublicLeagueLobbyNotification.class, BotTurnRequired.class, SocialEventNotification.class,
      ProfileEventNotification.class, CampaignEventNotification.class);

  private static final List<Class<? extends ApplicationEvent>> COORDINATION_EVENTS = List.of(
      MatchCompleted.class, MatchAbandoned.class, MatchForfeited.class,
      ResourceBecameUnjoinable.class);

  @Test
  @DisplayName("las notificaciones al usuario se emiten post-commit")
  void userNotificationsArePostCommit() {

    assertThat(USER_NOTIFICATION_EVENTS).allSatisfy(
        eventType -> assertThat(PostCommitApplicationEvent.class).isAssignableFrom(eventType));
  }

  @Test
  @DisplayName("los eventos de coordinacion con escrituras atomicas se procesan in-transaction")
  void coordinationEventsAreInTransaction() {

    assertThat(COORDINATION_EVENTS).allSatisfy(
        eventType -> assertThat(InTransactionApplicationEvent.class).isAssignableFrom(eventType));
  }

  @Test
  @DisplayName("ningun evento es a la vez post-commit e in-transaction")
  void noEventIsBothMarkers() {

    assertThat(USER_NOTIFICATION_EVENTS).noneSatisfy(
        eventType -> assertThat(InTransactionApplicationEvent.class).isAssignableFrom(eventType));
    assertThat(COORDINATION_EVENTS).noneSatisfy(
        eventType -> assertThat(PostCommitApplicationEvent.class).isAssignableFrom(eventType));
  }

}
