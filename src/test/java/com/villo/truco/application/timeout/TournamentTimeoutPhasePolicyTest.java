package com.villo.truco.application.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Policies de fase de timeout de torneo (liga/copa)")
class TournamentTimeoutPhasePolicyTest {

  private final LeagueTimeoutPhasePolicy leaguePolicy = new LeagueTimeoutPhasePolicy();
  private final CupTimeoutPhasePolicy cupPolicy = new CupTimeoutPhasePolicy();

  @Test
  @DisplayName("Liga: solo la sala de espera tiene fase LOBBY; el resto NONE")
  void leaguePhases() {

    assertThat(leaguePolicy.phaseOf(LeagueStatus.WAITING_FOR_PLAYERS)).isEqualTo(
        TimeoutPhase.LOBBY);
    assertThat(leaguePolicy.phaseOf(LeagueStatus.WAITING_FOR_START)).isEqualTo(TimeoutPhase.LOBBY);
    assertThat(leaguePolicy.phaseOf(LeagueStatus.IN_PROGRESS)).isEqualTo(TimeoutPhase.NONE);
    assertThat(leaguePolicy.phaseOf(LeagueStatus.FINISHED)).isEqualTo(TimeoutPhase.NONE);
    assertThat(leaguePolicy.phaseOf(LeagueStatus.CANCELLED)).isEqualTo(TimeoutPhase.NONE);
  }

  @Test
  @DisplayName("Copa: solo la sala de espera tiene fase LOBBY; el resto NONE")
  void cupPhases() {

    assertThat(cupPolicy.phaseOf(CupStatus.WAITING_FOR_PLAYERS)).isEqualTo(TimeoutPhase.LOBBY);
    assertThat(cupPolicy.phaseOf(CupStatus.WAITING_FOR_START)).isEqualTo(TimeoutPhase.LOBBY);
    assertThat(cupPolicy.phaseOf(CupStatus.IN_PROGRESS)).isEqualTo(TimeoutPhase.NONE);
    assertThat(cupPolicy.phaseOf(CupStatus.FINISHED)).isEqualTo(TimeoutPhase.NONE);
    assertThat(cupPolicy.phaseOf(CupStatus.CANCELLED)).isEqualTo(TimeoutPhase.NONE);
  }

}
