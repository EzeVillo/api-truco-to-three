package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.eventhandlers.MatchEventMapper;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchEventMapper")
class MatchWsEventTest {

  private final MatchEventMapper mapper = new MatchEventMapper();

  @Test
  @DisplayName("mapea eventos con payload esperado")
  void mapsPayload() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var readyPayload = mapper.map(
        new PlayerReadyEvent(matchId, p1, p2, PlayerSeat.PLAYER_ONE));
    final var joinedPayload = mapper.map(new PlayerJoinedEvent(matchId, p1, p2));
    final var finishedPayload = mapper.map(
        new MatchFinishedEvent(matchId, p1, p2, PlayerSeat.PLAYER_TWO, 1, 2));

    assertThat(readyPayload).containsEntry("seat", "PLAYER_ONE");
    assertThat(joinedPayload).isEmpty();
    assertThat(finishedPayload).containsEntry("winnerSeat", "PLAYER_TWO");
    assertThat(finishedPayload).containsEntry("gamesWonPlayerOne", 1);
    assertThat(finishedPayload).containsEntry("gamesWonPlayerTwo", 2);
  }

}
