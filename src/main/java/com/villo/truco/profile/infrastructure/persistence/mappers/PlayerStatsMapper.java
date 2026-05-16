package com.villo.truco.profile.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.model.PlayerStatsRehydrator;
import com.villo.truco.profile.domain.model.PlayerStatsSnapshot;
import com.villo.truco.profile.infrastructure.persistence.entities.PlayerStatsJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PlayerStatsMapper {

  public PlayerStatsJpaEntity toEntity(final PlayerStats stats) {

    final var snapshot = stats.snapshot();
    final var entity = new PlayerStatsJpaEntity();
    entity.setPlayerId(snapshot.playerId().value());
    entity.setMatchesPlayed(snapshot.matchesPlayed());
    entity.setMatchesWon(snapshot.matchesWon());
    entity.setMatchesLost(snapshot.matchesLost());
    entity.setVersion((int) stats.getVersion());
    return entity;
  }

  public PlayerStats toDomain(final PlayerStatsJpaEntity entity) {

    final var snapshot = new PlayerStatsSnapshot(new PlayerId(entity.getPlayerId()),
        entity.getMatchesPlayed(), entity.getMatchesWon(), entity.getMatchesLost());
    final var stats = PlayerStatsRehydrator.rehydrate(snapshot);
    stats.setVersion(entity.getVersion());
    return stats;
  }

}
