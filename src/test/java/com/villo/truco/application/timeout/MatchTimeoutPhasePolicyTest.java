package com.villo.truco.application.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchTimeoutPhasePolicy")
class MatchTimeoutPhasePolicyTest {

  private final MatchTimeoutPhasePolicy policy = new MatchTimeoutPhasePolicy();
  private final MatchId matchId = MatchId.generate();
  private final PlayerId playerOne = PlayerId.generate();
  private final PlayerId playerTwo = PlayerId.generate();

  @Test
  @DisplayName("La sala de espera mapea a LOBBY")
  void waitingStatesAreLobby() {

    assertThat(policy.phaseOf(MatchStatus.WAITING_FOR_PLAYERS)).isEqualTo(TimeoutPhase.LOBBY);
    assertThat(policy.phaseOf(MatchStatus.READY)).isEqualTo(TimeoutPhase.LOBBY);
  }

  @Test
  @DisplayName("La partida en curso mapea a PLAY")
  void inProgressIsPlay() {

    assertThat(policy.phaseOf(MatchStatus.IN_PROGRESS)).isEqualTo(TimeoutPhase.PLAY);
  }

  @Test
  @DisplayName("Los estados terminales mapean a NONE")
  void terminalStatesAreNone() {

    assertThat(policy.phaseOf(MatchStatus.FINISHED)).isEqualTo(TimeoutPhase.NONE);
    assertThat(policy.phaseOf(MatchStatus.CANCELLED)).isEqualTo(TimeoutPhase.NONE);
  }

  @Test
  @DisplayName("Eventos de lobby mapean a LOBBY")
  void lobbyEventsAreLobby() {

    assertThat(policy.phaseOf(new PlayerJoinedEvent(matchId, playerOne, null))).isEqualTo(
        TimeoutPhase.LOBBY);
  }

  @Test
  @DisplayName("Eventos de juego mapean a PLAY")
  void gameEventsArePlay() {

    assertThat(policy.phaseOf(new GameStartedEvent(matchId, playerOne, playerTwo, 1))).isEqualTo(
        TimeoutPhase.PLAY);
  }

  @Test
  @DisplayName("Eventos terminales mapean a NONE")
  void terminalEventsAreNone() {

    assertThat(policy.phaseOf(
        new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3,
            1))).isEqualTo(TimeoutPhase.NONE);
    assertThat(policy.phaseOf(new MatchCancelledEvent(matchId, playerOne, null))).isEqualTo(
        TimeoutPhase.NONE);
  }

}
