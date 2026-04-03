package com.villo.truco.infrastructure.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.SpectatorshipStopReason;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemorySpectatorshipRepository")
class InMemorySpectatorshipRepositoryTest {

  private InMemorySpectatorshipRepository repository;

  @BeforeEach
  void setUp() {

    this.repository = new InMemorySpectatorshipRepository();
  }

  @Test
  @DisplayName("guarda y consulta spectatorship activa")
  void saveAndQueryActiveSpectatorship() {

    final var matchId = MatchId.generate();
    final var spectator = PlayerId.generate();
    final var spectatorship = Spectatorship.create(spectator);
    spectatorship.startWatching(matchId);

    this.repository.save(spectatorship);

    assertThat(this.repository.findBySpectatorId(spectator)).isPresent();
    assertThat(this.repository.countActiveByMatchId(matchId)).isEqualTo(1);
    assertThat(this.repository.findActiveSpectatorIdsByMatchId(matchId)).containsExactly(spectator);
    assertThat(this.repository.findActiveByMatchId(matchId)).hasSize(1);
  }

  @Test
  @DisplayName("no cuenta spectatorships inactivas")
  void ignoresInactiveSpectatorships() {

    final var matchId = MatchId.generate();
    final var spectator = PlayerId.generate();
    final var spectatorship = Spectatorship.create(spectator);
    spectatorship.startWatching(matchId);
    spectatorship.stopWatching(SpectatorshipStopReason.MANUAL);

    this.repository.save(spectatorship);

    assertThat(this.repository.countActiveByMatchId(matchId)).isZero();
    assertThat(this.repository.findActiveSpectatorIdsByMatchId(matchId)).isEmpty();
  }

}
