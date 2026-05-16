package com.villo.truco.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerStats")
class PlayerStatsTest {

  @Test
  @DisplayName("recordOutcome(WON) incrementa matchesPlayed y matchesWon")
  void recordWonOutcome() {

    final var stats = PlayerStats.create(PlayerId.generate());

    stats.recordOutcome(MatchOutcome.WON);

    assertThat(stats.matchesPlayed()).isEqualTo(1);
    assertThat(stats.matchesWon()).isEqualTo(1);
    assertThat(stats.matchesLost()).isEqualTo(0);
  }

  @Test
  @DisplayName("recordOutcome(LOST) incrementa matchesPlayed y matchesLost")
  void recordLostOutcome() {

    final var stats = PlayerStats.create(PlayerId.generate());

    stats.recordOutcome(MatchOutcome.LOST);

    assertThat(stats.matchesPlayed()).isEqualTo(1);
    assertThat(stats.matchesWon()).isEqualTo(0);
    assertThat(stats.matchesLost()).isEqualTo(1);
  }

  @Test
  @DisplayName("winRate devuelve 0 cuando matchesPlayed es 0")
  void winRateIsZeroWhenNoMatches() {

    final var stats = PlayerStats.create(PlayerId.generate());

    assertThat(stats.winRate()).isEqualTo(0);
  }

  @Test
  @DisplayName("winRate devuelve el porcentaje de victorias redondeado sin decimales")
  void winRateCalculation() {

    final var stats = PlayerStats.create(PlayerId.generate());
    stats.recordOutcome(MatchOutcome.WON);
    stats.recordOutcome(MatchOutcome.WON);
    stats.recordOutcome(MatchOutcome.LOST);

    assertThat(stats.winRate()).isEqualTo(67);
  }

  @Test
  @DisplayName("snapshot y rehydrate son inversos")
  void snapshotAndRehydrateRoundTrip() {

    final var playerId = PlayerId.generate();
    final var stats = PlayerStats.create(playerId);
    stats.recordOutcome(MatchOutcome.WON);
    stats.recordOutcome(MatchOutcome.LOST);

    final var rehydrated = PlayerStatsRehydrator.rehydrate(stats.snapshot());

    assertThat(rehydrated.getId()).isEqualTo(playerId);
    assertThat(rehydrated.matchesPlayed()).isEqualTo(2);
    assertThat(rehydrated.matchesWon()).isEqualTo(1);
    assertThat(rehydrated.matchesLost()).isEqualTo(1);
  }

}
