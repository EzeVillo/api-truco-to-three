package com.villo.truco.social.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import com.villo.truco.social.infrastructure.persistence.mappers.SocialPreferencesMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataSocialPreferencesRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaSocialPreferencesRepositoryAdapter implements SocialPreferencesRepository {

  private final SpringDataSocialPreferencesRepository springDataSocialPreferencesRepository;
  private final SocialPreferencesMapper socialPreferencesMapper;

  public JpaSocialPreferencesRepositoryAdapter(
      final SpringDataSocialPreferencesRepository springDataSocialPreferencesRepository,
      final SocialPreferencesMapper socialPreferencesMapper) {

    this.springDataSocialPreferencesRepository = springDataSocialPreferencesRepository;
    this.socialPreferencesMapper = socialPreferencesMapper;
  }

  @Override
  @Transactional
  public void save(final SocialPreferences socialPreferences) {

    try {
      final var entity = this.socialPreferencesMapper.toEntity(socialPreferences);
      this.springDataSocialPreferencesRepository.saveAndFlush(entity);
      socialPreferences.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "SocialPreferences " + socialPreferences.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<SocialPreferences> findByPlayerId(final PlayerId playerId) {

    return this.springDataSocialPreferencesRepository.findById(playerId.value())
        .map(this.socialPreferencesMapper::toDomain);
  }

}
