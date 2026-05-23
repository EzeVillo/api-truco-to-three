package com.villo.truco.domain.model.quickmatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuickMatchTicket")
class QuickMatchTicketTest {

  @Test
  @DisplayName("null playerId → NullPointerException")
  void nullPlayerIdThrows() {

    assertThatThrownBy(
        () -> new QuickMatchTicket(null, GamesToPlay.of(3), Instant.now(), null)).isInstanceOf(
        NullPointerException.class);
  }

  @Test
  @DisplayName("null gamesToPlay → NullPointerException")
  void nullGamesToPlayThrows() {

    assertThatThrownBy(
        () -> new QuickMatchTicket(PlayerId.generate(), null, Instant.now(), null)).isInstanceOf(
        NullPointerException.class);
  }

  @Test
  @DisplayName("valid construction succeeds and fields are accessible")
  void validConstructionSucceeds() {

    final var playerId = PlayerId.generate();
    final var gamesToPlay = GamesToPlay.of(3);
    final var enqueuedAt = Instant.now();
    final var sessionId = "session-123";

    final var ticket = new QuickMatchTicket(playerId, gamesToPlay, enqueuedAt, sessionId);

    assertThat(ticket.playerId()).isEqualTo(playerId);
    assertThat(ticket.gamesToPlay()).isEqualTo(gamesToPlay);
    assertThat(ticket.enqueuedAt()).isEqualTo(enqueuedAt);
    assertThat(ticket.webSocketSessionId()).isEqualTo(sessionId);
  }

  @Test
  @DisplayName("null webSocketSessionId is allowed")
  void nullSessionIdIsAllowed() {

    final var ticket = new QuickMatchTicket(PlayerId.generate(), GamesToPlay.of(3), Instant.now(),
        null);

    assertThat(ticket.webSocketSessionId()).isNull();
  }

}
