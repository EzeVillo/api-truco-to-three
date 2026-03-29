package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchRecipientResolver")
class MatchRecipientResolverTest {

  private final MatchRecipientResolver resolver = new MatchRecipientResolver();

  @Test
  @DisplayName("evento broadcast → devuelve ambos jugadores")
  void broadcastEventReturnsBothPlayers() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final MatchDomainEvent outer = new PlayerJoinedEvent(matchId, p1, p2);

    final var recipients = resolver.resolve(outer, outer);

    assertThat(recipients).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  @DisplayName("SeatTargetedEvent PLAYER_TWO → devuelve solo playerTwo")
  void seatTargetedEventReturnsSinglePlayer() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final MatchDomainEvent outer = new PlayerJoinedEvent(matchId, p1, p2);
    final var inner = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_TWO, List.of());

    final var recipients = resolver.resolve(outer, inner);

    assertThat(recipients).containsExactly(p2);
  }

  @Test
  @DisplayName("SeatTargetedEvent PLAYER_ONE → devuelve solo playerOne")
  void seatTargetedPlayerOneReturnPlayerOne() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final MatchDomainEvent outer = new PlayerJoinedEvent(matchId, p1, p2);
    final var inner = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of());

    final var recipients = resolver.resolve(outer, inner);

    assertThat(recipients).containsExactly(p1);
  }

  @Test
  @DisplayName("estado WAITING (playerTwo null) → devuelve solo playerOne")
  void waitingStateWithNullPlayerTwoReturnsSinglePlayer() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final MatchDomainEvent outer = new PlayerJoinedEvent(matchId, p1, null);

    final var recipients = resolver.resolve(outer, outer);

    assertThat(recipients).containsExactly(p1);
  }

}
