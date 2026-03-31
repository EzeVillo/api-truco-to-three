package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.events.CardPlayedEvent;
import com.villo.truco.domain.model.match.events.HandResolvedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchEventMapper")
class MatchEventMapperTest {

  private final MatchEventMapper mapper = new MatchEventMapper();

  @Test
  @DisplayName("TurnChangedEvent → payload con seat")
  void turnChangedMapsToSeat() {

    final var payload = mapper.map(new TurnChangedEvent(PlayerSeat.PLAYER_TWO));

    assertThat(payload).containsEntry("seat", "PLAYER_TWO");
  }

  @Test
  @DisplayName("CardPlayedEvent → payload con seat y card")
  void cardPlayedMapsSeatAndCard() {

    final var card = Card.of(Suit.ESPADA, 1);
    final var payload = mapper.map(new CardPlayedEvent(PlayerSeat.PLAYER_ONE, card));

    assertThat(payload).containsEntry("seat", "PLAYER_ONE");
    @SuppressWarnings("unchecked") final var cardMap = (java.util.Map<String, Object>) payload.get(
        "card");
    assertThat(cardMap).containsEntry("suit", "ESPADA").containsEntry("number", 1);
  }

  @Test
  @DisplayName("HandResolvedEvent con carta rival nula a payload serializable")
  void handResolvedMapsNullCard() {

    final var card = Card.of(Suit.ESPADA, 1);
    final var payload = mapper.map(new HandResolvedEvent(card, null, PlayerSeat.PLAYER_ONE));

    assertThat(payload).containsEntry("winnerSeat", "PLAYER_ONE");
    @SuppressWarnings("unchecked") final var cardMap = (java.util.Map<String, Object>) payload.get(
        "cardPlayerOne");
    assertThat(cardMap).containsEntry("suit", "ESPADA").containsEntry("number", 1);
    assertThat(payload).containsEntry("cardPlayerTwo", null);
  }

  @Test
  @DisplayName("MatchFinishedEvent → payload con winnerSeat y juegos")
  void matchFinishedMapsCorrectly() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var payload = mapper.map(
        new MatchFinishedEvent(matchId, p1, p2, PlayerSeat.PLAYER_ONE, 3, 1));

    assertThat(payload).containsEntry("winnerSeat", "PLAYER_ONE")
        .containsEntry("gamesWonPlayerOne", 3).containsEntry("gamesWonPlayerTwo", 1);
  }

  @Test
  @DisplayName("MatchAbandonedEvent - payload con winnerSeat, abandonerSeat y juegos")
  void matchAbandonedMapsCorrectly() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var payload = mapper.map(
        new MatchAbandonedEvent(matchId, p1, p2, PlayerSeat.PLAYER_TWO, PlayerSeat.PLAYER_ONE, 0,
            3));

    assertThat(payload).containsEntry("winnerSeat", "PLAYER_TWO")
        .containsEntry("abandonerSeat", "PLAYER_ONE").containsEntry("gamesWonPlayerOne", 0)
        .containsEntry("gamesWonPlayerTwo", 3);
  }

  @Test
  @DisplayName("MatchForfeitedEvent - payload con winnerSeat y loserSeat")
  void matchForfeitedMapsCorrectly() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var payload = mapper.map(
        new MatchForfeitedEvent(matchId, p1, p2, PlayerSeat.PLAYER_ONE, 3, 1));

    assertThat(payload).containsEntry("winnerSeat", "PLAYER_ONE")
        .containsEntry("loserSeat", "PLAYER_TWO").containsEntry("gamesWonPlayerOne", 3)
        .containsEntry("gamesWonPlayerTwo", 1);
  }

  @Test
  @DisplayName("PlayerJoinedEvent → payload vacío")
  void playerJoinedMapsToEmptyPayload() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var payload = mapper.map(new PlayerJoinedEvent(matchId, p1, p2));

    assertThat(payload).isEmpty();
  }

}
