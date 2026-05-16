package com.villo.truco.profile.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import com.villo.truco.profile.infrastructure.persistence.mappers.PlayerStatsMapper;
import com.villo.truco.profile.infrastructure.persistence.repositories.spring.SpringDataPlayerStatsRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaPlayerStatsRepositoryAdapter implements PlayerStatsRepository {

  private final SpringDataPlayerStatsRepository springDataPlayerStatsRepository;
  private final PlayerStatsMapper playerStatsMapper;

  public JpaPlayerStatsRepositoryAdapter(
      final SpringDataPlayerStatsRepository springDataPlayerStatsRepository,
      final PlayerStatsMapper playerStatsMapper) {

    this.springDataPlayerStatsRepository = springDataPlayerStatsRepository;
    this.playerStatsMapper = playerStatsMapper;
  }

  @Override
  @Transactional
  public void save(final PlayerStats stats) {

    try {
      final var entity = this.playerStatsMapper.toEntity(stats);
      this.springDataPlayerStatsRepository.saveAndFlush(entity);
      stats.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "PlayerStats " + stats.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<PlayerStats> findByPlayerId(final PlayerId playerId) {

    return this.springDataPlayerStatsRepository.findById(playerId.value())
        .map(this.playerStatsMapper::toDomain);
  }

}
