package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchNotificationTranslator")
class MatchNotificationEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final MatchNotificationEventTranslator translator = new MatchNotificationEventTranslator(
      new MatchEventMapper(), new MatchRecipientResolver(), publisher);

  @Test
  @DisplayName("evento directo → publica MatchEventNotification con matchId y recipients")
  void directEventPublishesNotification() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var event = new PlayerJoinedEvent(matchId, p1, p2);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.get(0);
    assertThat(notification.matchId()).isEqualTo(matchId);
    assertThat(notification.eventType()).isEqualTo("PLAYER_JOINED");
    assertThat(notification.recipients()).containsExactlyInAnyOrder(p1, p2);
    assertThat(notification.payload()).isEmpty();
  }

  @Test
  @DisplayName("MatchEventEnvelope con SeatTargetedEvent → unwraps, recipient es el del seat")
  void envelopeUnwrapsSeatTargetedEvent() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var inner = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of());
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);

    translator.handle(envelope);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.get(0);
    assertThat(notification.matchId()).isEqualTo(matchId);
    assertThat(notification.eventType()).isEqualTo("PLAYER_HAND_UPDATED");
    assertThat(notification.recipients()).containsExactly(p1);
  }

  @Test
  @DisplayName("MatchEventEnvelope con broadcast event → ambos jugadores como recipients")
  void envelopeBroadcastSendsToBoothPlayers() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var inner = new PlayerJoinedEvent(matchId, p1, p2);
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);

    translator.handle(envelope);

    final var notification = (MatchEventNotification) published.get(0);
    assertThat(notification.recipients()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  @DisplayName("MatchAbandonedEvent - publica MATCH_ABANDONED con payload mapeado")
  void matchAbandonedPublishesNotification() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var event = new MatchAbandonedEvent(matchId, p1, p2, PlayerSeat.PLAYER_TWO,
        PlayerSeat.PLAYER_ONE, 0, 3);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.getFirst();
    assertThat(notification.eventType()).isEqualTo("MATCH_ABANDONED");
    assertThat(notification.recipients()).containsExactlyInAnyOrder(p1, p2);
    assertThat(notification.payload()).containsEntry("winnerSeat", "PLAYER_TWO")
        .containsEntry("abandonerSeat", "PLAYER_ONE");
  }

}
