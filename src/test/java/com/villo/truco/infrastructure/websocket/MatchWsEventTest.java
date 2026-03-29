package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchWsEvent")
class MatchWsEventTest {

  @Test
  @DisplayName("preserva shape con matchId y payload")
  void preservesShape() {

    final var event = new MatchWsEvent("match-1", "PLAYER_READY", 123L,
        Map.of("seat", "PLAYER_ONE"));

    assertThat(event.matchId()).isEqualTo("match-1");
    assertThat(event.eventType()).isEqualTo("PLAYER_READY");
    assertThat(event.timestamp()).isEqualTo(123L);
    assertThat(event.payload()).containsEntry("seat", "PLAYER_ONE");
  }

}
