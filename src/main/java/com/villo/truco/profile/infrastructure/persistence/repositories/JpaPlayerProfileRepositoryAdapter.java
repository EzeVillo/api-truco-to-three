package com.villo.truco.profile.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.infrastructure.persistence.mappers.PlayerProfileMapper;
import com.villo.truco.profile.infrastructure.persistence.repositories.spring.SpringDataPlayerProfileRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaPlayerProfileRepositoryAdapter implements PlayerProfileRepository {

  private final SpringDataPlayerProfileRepository springDataPlayerProfileRepository;
  private final PlayerProfileMapper playerProfileMapper;

  public JpaPlayerProfileRepositoryAdapter(
      final SpringDataPlayerProfileRepository springDataPlayerProfileRepository,
      final PlayerProfileMapper playerProfileMapper) {

    this.springDataPlayerProfileRepository = springDataPlayerProfileRepository;
    this.playerProfileMapper = playerProfileMapper;
  }

  @Override
  @Transactional
  public void save(final PlayerProfile profile) {

    try {
      final var entity = this.playerProfileMapper.toEntity(profile);
      this.springDataPlayerProfileRepository.saveAndFlush(entity);
      profile.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "PlayerProfile " + profile.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<PlayerProfile> findByPlayerId(final PlayerId playerId) {

    return this.springDataPlayerProfileRepository.findById(playerId.value())
        .map(this.playerProfileMapper::toDomain);
  }
}
