package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.MatchJpaEntity;
import com.villo.truco.infrastructure.persistence.mappers.MatchMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DisplayName("JpaMatchRepositoryAdapter")
class JpaMatchRepositoryAdapterTest {

  @Test
  @DisplayName("save actualiza version")
  void saveUpdatesVersion() {

    final var springRepo = mock(SpringDataMatchRepository.class);
    final var mapper = mock(MatchMapper.class);
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper);

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    final var entity = new MatchJpaEntity();
    entity.setVersion(7);
    when(mapper.toEntity(match)).thenReturn(entity);

    adapter.save(match);

    verify(springRepo).saveAndFlush(entity);
    assertThat(match.getVersion()).isEqualTo(7);
  }

  @Test
  @DisplayName("save traduce optimistic lock")
  void saveTranslatesOptimisticLock() {

    final var springRepo = mock(SpringDataMatchRepository.class);
    final var mapper = mock(MatchMapper.class);
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper);
    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    when(mapper.toEntity(match)).thenReturn(new MatchJpaEntity());
    when(springRepo.saveAndFlush(any())).thenThrow(
        new ObjectOptimisticLockingFailureException("Match", match.getId().value()));

    assertThatThrownBy(() -> adapter.save(match)).isInstanceOf(StaleAggregateException.class);
  }

}
