package com.villo.truco.history.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.MatchEndReason;
import com.villo.truco.history.domain.model.MatchHistoryEntry;
import com.villo.truco.history.domain.model.MatchOutcome;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import com.villo.truco.history.infrastructure.persistence.entities.PlayerMatchHistoryJpaEntity;
import com.villo.truco.history.infrastructure.persistence.entities.PlayerMatchHistoryStateData;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlayerMatchHistoryMapper {

  public PlayerMatchHistoryJpaEntity toEntity(final PlayerMatchHistory history) {

    final var snapshot = history.snapshot();
    final var entity = new PlayerMatchHistoryJpaEntity();
    entity.setPlayerId(snapshot.playerId().value());
    entity.setState(this.toStateData(snapshot.entries()));
    entity.setVersion((int) history.getVersion());
    return entity;
  }

  public PlayerMatchHistory toDomain(final PlayerMatchHistoryJpaEntity entity) {

    final var entries = entity.getState().entries().stream().map(this::toEntry).toList();
    final var history = PlayerMatchHistory.reconstruct(new PlayerId(entity.getPlayerId()), entries);
    history.setVersion(entity.getVersion());
    return history;
  }

  private PlayerMatchHistoryStateData toStateData(final List<MatchHistoryEntry> entries) {

    final var entryData = entries.stream().map(
            entry -> new PlayerMatchHistoryStateData.EntryData(entry.matchId().value(),
                entry.opponentId().value(), entry.outcome().name(), entry.endReason().name(),
                entry.ownGamesWon(), entry.opponentGamesWon(), entry.endedAt().toEpochMilli()))
        .toList();
    return new PlayerMatchHistoryStateData(entryData);
  }

  private MatchHistoryEntry toEntry(final PlayerMatchHistoryStateData.EntryData data) {

    return new MatchHistoryEntry(new MatchId(data.matchId()), new PlayerId(data.opponentId()),
        MatchOutcome.valueOf(data.outcome()), MatchEndReason.valueOf(data.endReason()),
        data.ownGamesWon(), data.opponentGamesWon(),
        Instant.ofEpochMilli(data.endedAtEpochMilli()));
  }

}
