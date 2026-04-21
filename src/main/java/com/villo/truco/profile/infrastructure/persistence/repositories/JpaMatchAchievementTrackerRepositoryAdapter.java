package com.villo.truco.profile.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.profile.domain.model.MatchAchievementTracker;
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.infrastructure.persistence.mappers.MatchAchievementTrackerMapper;
import com.villo.truco.profile.infrastructure.persistence.repositories.spring.SpringDataMatchAchievementTrackerRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaMatchAchievementTrackerRepositoryAdapter implements
    MatchAchievementTrackerRepository {

  private final SpringDataMatchAchievementTrackerRepository springDataMatchAchievementTrackerRepository;
  private final MatchAchievementTrackerMapper matchAchievementTrackerMapper;

  public JpaMatchAchievementTrackerRepositoryAdapter(
      final SpringDataMatchAchievementTrackerRepository springDataMatchAchievementTrackerRepository,
      final MatchAchievementTrackerMapper matchAchievementTrackerMapper) {

    this.springDataMatchAchievementTrackerRepository = springDataMatchAchievementTrackerRepository;
    this.matchAchievementTrackerMapper = matchAchievementTrackerMapper;
  }

  @Override
  @Transactional
  public void save(final MatchAchievementTracker tracker) {

    try {
      final var entity = this.matchAchievementTrackerMapper.toEntity(tracker);
      this.springDataMatchAchievementTrackerRepository.saveAndFlush(entity);
      tracker.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "MatchAchievementTracker " + tracker.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<MatchAchievementTracker> findByMatchId(final MatchId matchId) {

    return this.springDataMatchAchievementTrackerRepository.findById(matchId.value())
        .map(this.matchAchievementTrackerMapper::toDomain);
  }

  @Override
  @Transactional
  public void deleteByMatchId(final MatchId matchId) {

    this.springDataMatchAchievementTrackerRepository.deleteById(matchId.value());
  }
}
