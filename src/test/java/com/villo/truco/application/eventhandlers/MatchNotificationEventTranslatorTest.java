package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.match.events.HandDealtEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  @DisplayName("HandDealtEvent genera dos notificaciones con mismo stateVersion y payload redactado")
  void handDealtGeneratesTwoRedactedNotificationsWithSameStateVersion() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var cardsP1 = List.of(Card.of(Suit.ORO, 1), Card.of(Suit.BASTO, 7));
    final var cardsP2 = List.of(Card.of(Suit.COPA, 3), Card.of(Suit.ESPADA, 1));
    final var inner = new HandDealtEvent(
        Map.of(PlayerSeat.PLAYER_ONE, cardsP1, PlayerSeat.PLAYER_TWO, cardsP2));
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);
    envelope.setStateVersion(5L);

    translator.handle(envelope);

    assertThat(published).hasSize(2);

    final var n1 = (MatchEventNotification) published.get(0);
    final var n2 = (MatchEventNotification) published.get(1);

    assertThat(n1.stateVersion()).isEqualTo(5L);
    assertThat(n2.stateVersion()).isEqualTo(5L);
    assertThat(n1.eventType()).isEqualTo("HAND_DEALT");
    assertThat(n2.eventType()).isEqualTo("HAND_DEALT");

    final var recipient1 = n1.recipients().getFirst();
    final var recipient2 = n2.recipients().getFirst();
    final var payload1 = n1.payload();
    final var payload2 = n2.payload();

    assertThat(recipient1).isNotEqualTo(recipient2);
    assertThat(List.of(recipient1, recipient2)).containsExactlyInAnyOrder(p1, p2);

    final var cardsPayload1 = (List<?>) payload1.get("cards");
    final var cardsPayload2 = (List<?>) payload2.get("cards");

    assertThat(cardsPayload1).hasSize(2);
    assertThat(cardsPayload2).hasSize(2);

    assertThat(payload1).doesNotContainKey(PlayerSeat.PLAYER_TWO.name().toLowerCase());
    assertThat(payload2).doesNotContainKey(PlayerSeat.PLAYER_ONE.name().toLowerCase());
  }

  @Test
  @DisplayName("Eventos derivados publican MatchEventNotification con stateVersion null")
  void derivedEventPublishesNotificationWithNullStateVersion() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var inner = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of());
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);
    envelope.setStateVersion(3L);

    translator.handle(envelope);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.get(0);
    assertThat(notification.eventType()).isEqualTo("PLAYER_HAND_UPDATED");
    assertThat(notification.stateVersion()).isNull();
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
