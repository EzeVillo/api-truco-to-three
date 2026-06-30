package com.villo.truco.social.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.infrastructure.persistence.entities.SocialPreferencesJpaEntity;
import com.villo.truco.social.infrastructure.persistence.mappers.SocialPreferencesMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataSocialPreferencesRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DisplayName("JpaSocialPreferencesRepositoryAdapter")
class JpaSocialPreferencesRepositoryAdapterTest {

  @Test
  @DisplayName("save actualiza version del agregado")
  void saveUpdatesVersion() {

    final var springRepo = mock(SpringDataSocialPreferencesRepository.class);
    final var mapper = mock(SocialPreferencesMapper.class);
    final var adapter = new JpaSocialPreferencesRepositoryAdapter(springRepo, mapper);
    final var preferences = SocialPreferences.create(PlayerId.generate());
    final var entity = new SocialPreferencesJpaEntity();
    entity.setVersion(7);
    when(mapper.toEntity(preferences)).thenReturn(entity);

    adapter.save(preferences);

    verify(springRepo).saveAndFlush(entity);
    assertThat(preferences.getVersion()).isEqualTo(7);
  }

  @Test
  @DisplayName("save traduce optimistic lock")
  void saveTranslatesOptimisticLock() {

    final var springRepo = mock(SpringDataSocialPreferencesRepository.class);
    final var mapper = mock(SocialPreferencesMapper.class);
    final var adapter = new JpaSocialPreferencesRepositoryAdapter(springRepo, mapper);
    final var preferences = SocialPreferences.create(PlayerId.generate());
    when(mapper.toEntity(preferences)).thenReturn(new SocialPreferencesJpaEntity());
    when(springRepo.saveAndFlush(any())).thenThrow(
        new ObjectOptimisticLockingFailureException("SocialPreferences",
            preferences.getId().value()));

    assertThatThrownBy(() -> adapter.save(preferences)).isInstanceOf(StaleAggregateException.class);
  }

  @Test
  @DisplayName("findByPlayerId mapea la entidad a dominio")
  void findByPlayerIdMapsToDomain() {

    final var springRepo = mock(SpringDataSocialPreferencesRepository.class);
    final var mapper = mock(SocialPreferencesMapper.class);
    final var adapter = new JpaSocialPreferencesRepositoryAdapter(springRepo, mapper);
    final var playerId = PlayerId.generate();
    final var entity = new SocialPreferencesJpaEntity();
    entity.setPlayerId(playerId.value());
    final var domain = SocialPreferences.reconstruct(playerId, false);
    when(springRepo.findById(playerId.value())).thenReturn(Optional.of(entity));
    when(mapper.toDomain(entity)).thenReturn(domain);

    final var result = adapter.findByPlayerId(playerId);

    assertThat(result).containsSame(domain);
  }

  @Test
  @DisplayName("findByPlayerId devuelve vacio cuando no existe")
  void findByPlayerIdReturnsEmptyWhenMissing() {

    final var springRepo = mock(SpringDataSocialPreferencesRepository.class);
    final var mapper = mock(SocialPreferencesMapper.class);
    final var adapter = new JpaSocialPreferencesRepositoryAdapter(springRepo, mapper);
    final var playerId = PlayerId.generate();
    when(springRepo.findById(playerId.value())).thenReturn(Optional.empty());

    final var result = adapter.findByPlayerId(playerId);

    assertThat(result).isEmpty();
  }

}
