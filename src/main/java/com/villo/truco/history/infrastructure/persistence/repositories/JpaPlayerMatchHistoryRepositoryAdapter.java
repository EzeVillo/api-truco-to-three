package com.villo.truco.history.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import com.villo.truco.history.infrastructure.persistence.mappers.PlayerMatchHistoryMapper;
import com.villo.truco.history.infrastructure.persistence.repositories.spring.SpringDataPlayerMatchHistoryRepository;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaPlayerMatchHistoryRepositoryAdapter implements PlayerMatchHistoryRepository {

  private final SpringDataPlayerMatchHistoryRepository springDataPlayerMatchHistoryRepository;
  private final PlayerMatchHistoryMapper playerMatchHistoryMapper;

  public JpaPlayerMatchHistoryRepositoryAdapter(
      final SpringDataPlayerMatchHistoryRepository springDataPlayerMatchHistoryRepository,
      final PlayerMatchHistoryMapper playerMatchHistoryMapper) {

    this.springDataPlayerMatchHistoryRepository = springDataPlayerMatchHistoryRepository;
    this.playerMatchHistoryMapper = playerMatchHistoryMapper;
  }

  @Override
  @Transactional
  public void save(final PlayerMatchHistory history) {

    try {
      final var entity = this.playerMatchHistoryMapper.toEntity(history);
      this.springDataPlayerMatchHistoryRepository.saveAndFlush(entity);
      history.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "PlayerMatchHistory " + history.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<PlayerMatchHistory> findByPlayerId(final PlayerId playerId) {

    return this.springDataPlayerMatchHistoryRepository.findById(playerId.value())
        .map(this.playerMatchHistoryMapper::toDomain);
  }

}
