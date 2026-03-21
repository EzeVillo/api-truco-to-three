package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchWsEvent")
class MatchWsEventTest {

  @Test
  @DisplayName("mapea eventos con payload esperado")
  void mapsPayload() {

    final var ready = MatchWsEvent.from(new PlayerReadyEvent(PlayerSeat.PLAYER_ONE));
    final var joined = MatchWsEvent.from(new PlayerJoinedEvent());
    final var finished = MatchWsEvent.from(new MatchFinishedEvent(PlayerSeat.PLAYER_TWO, 1, 2));

    assertThat(ready.eventType()).isEqualTo("PLAYER_READY");
    assertThat(ready.payload()).containsEntry("seat", "PLAYER_ONE");
    assertThat(joined.payload()).isEmpty();
    assertThat(finished.payload()).containsEntry("winnerSeat", "PLAYER_TWO");
    assertThat(finished.payload()).containsEntry("gamesWonPlayerOne", 1);
    assertThat(finished.payload()).containsEntry("gamesWonPlayerTwo", 2);
  }

}
