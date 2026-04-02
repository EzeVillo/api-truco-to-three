package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.LeagueEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.support.TestPublicActorResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueNotificationTranslator")
class LeagueNotificationEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final LeagueNotificationEventTranslator translator = new LeagueNotificationEventTranslator(
      new LeagueEventMapper(TestPublicActorResolver.guestStyle()), publisher);

  @Test
  @DisplayName("LeagueStartedEvent → publica LeagueEventNotification con todos los participantes")
  void leagueStartedPublishesNotificationWithAllParticipants() {

    final var leagueId = LeagueId.generate();
    final var participants = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());
    final var event = new LeagueStartedEvent(leagueId, participants);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (LeagueEventNotification) published.get(0);
    assertThat(notification.leagueId()).isEqualTo(leagueId);
    assertThat(notification.eventType()).isEqualTo("LEAGUE_STARTED");
    assertThat(notification.recipients()).containsExactlyInAnyOrderElementsOf(participants);
  }

}
