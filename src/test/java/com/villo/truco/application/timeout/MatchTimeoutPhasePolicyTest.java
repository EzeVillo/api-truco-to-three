package com.villo.truco.application.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchTimeoutPhasePolicy")
class MatchTimeoutPhasePolicyTest {

  private final MatchTimeoutPhasePolicy policy = new MatchTimeoutPhasePolicy();

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

}
