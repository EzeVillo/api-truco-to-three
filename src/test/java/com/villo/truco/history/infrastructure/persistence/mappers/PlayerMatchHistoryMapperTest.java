package com.villo.truco.history.infrastructure.persistence.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.MatchEndReason;
import com.villo.truco.history.domain.model.MatchHistoryEntry;
import com.villo.truco.history.domain.model.MatchOutcome;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerMatchHistoryMapper")
class PlayerMatchHistoryMapperTest {

  private final PlayerMatchHistoryMapper mapper = new PlayerMatchHistoryMapper();

  @Test
  @DisplayName("round-trip toEntity/toDomain preserva las entradas")
  void roundTripPreservesEntries() {

    final var player = PlayerId.generate();
    final var opponent = PlayerId.generate();
    final var matchId = MatchId.generate();
    final var endedAt = Instant.parse("2026-06-15T12:30:00Z");
    final var history = PlayerMatchHistory.create(player);
    history.record(
        new MatchHistoryEntry(matchId, opponent, MatchOutcome.LOST, MatchEndReason.ABANDONED, 1, 3,
            endedAt));

    final var entity = this.mapper.toEntity(history);
    final var restored = this.mapper.toDomain(entity);

    assertThat(entity.getPlayerId()).isEqualTo(player.value());
    assertThat(restored.getId()).isEqualTo(player);
    assertThat(restored.getEntries()).hasSize(1);
    final var entry = restored.getEntries().get(0);
    assertThat(entry.matchId()).isEqualTo(matchId);
    assertThat(entry.opponentId()).isEqualTo(opponent);
    assertThat(entry.outcome()).isEqualTo(MatchOutcome.LOST);
    assertThat(entry.endReason()).isEqualTo(MatchEndReason.ABANDONED);
    assertThat(entry.ownGamesWon()).isEqualTo(1);
    assertThat(entry.opponentGamesWon()).isEqualTo(3);
    assertThat(entry.endedAt()).isEqualTo(endedAt);
  }

}
