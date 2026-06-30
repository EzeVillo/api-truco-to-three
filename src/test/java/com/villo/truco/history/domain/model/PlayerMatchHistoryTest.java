package com.villo.truco.history.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerMatchHistory")
class PlayerMatchHistoryTest {

  private final PlayerId player = PlayerId.generate();

  private MatchHistoryEntry entry(final Instant endedAt) {

    return new MatchHistoryEntry(MatchId.generate(), PlayerId.generate(), MatchOutcome.WON,
        MatchEndReason.FINISHED, 3, 1, endedAt);
  }

  @Test
  @DisplayName("conserva como máximo 5 partidas, las más recientes")
  void capsToFiveMostRecent() {

    final var history = PlayerMatchHistory.create(this.player);
    final var base = Instant.parse("2026-06-01T00:00:00Z");

    for (int i = 0; i < 7; i++) {
      history.record(this.entry(base.plusSeconds(i)));
    }

    final var entries = history.getEntries();
    assertThat(entries).hasSize(5);
    assertThat(entries.get(0).endedAt()).isEqualTo(base.plusSeconds(6));
    assertThat(entries).extracting(MatchHistoryEntry::endedAt)
        .doesNotContain(base, base.plusSeconds(1));
  }

  @Test
  @DisplayName("ordena por fecha de fin descendente al registrar fuera de orden")
  void ordersByEndedAtDescending() {

    final var history = PlayerMatchHistory.create(this.player);
    final var older = this.entry(Instant.parse("2026-06-01T00:00:00Z"));
    final var newer = this.entry(Instant.parse("2026-06-02T00:00:00Z"));

    history.record(older);
    history.record(newer);

    assertThat(history.getEntries()).containsExactly(newer, older);
  }

  @Test
  @DisplayName("es idempotente: no registra dos veces la misma partida")
  void dedupesByMatchId() {

    final var history = PlayerMatchHistory.create(this.player);
    final var matchId = MatchId.generate();
    final var entry = new MatchHistoryEntry(matchId, PlayerId.generate(), MatchOutcome.WON,
        MatchEndReason.FINISHED, 3, 0, Instant.parse("2026-06-01T00:00:00Z"));

    assertThat(history.record(entry)).isTrue();
    assertThat(history.record(entry)).isFalse();
    assertThat(history.getEntries()).hasSize(1);
  }

  @Test
  @DisplayName("reconstruct preserva la invariante de orden y tope")
  void reconstructKeepsInvariant() {

    final var base = Instant.parse("2026-06-01T00:00:00Z");
    final var entries = List.of(this.entry(base), this.entry(base.plusSeconds(10)),
        this.entry(base.plusSeconds(5)), this.entry(base.plusSeconds(20)),
        this.entry(base.plusSeconds(1)), this.entry(base.plusSeconds(30)));

    final var history = PlayerMatchHistory.reconstruct(this.player, entries);

    assertThat(history.getEntries()).hasSize(5);
    assertThat(history.getEntries().get(0).endedAt()).isEqualTo(base.plusSeconds(30));
  }

}
