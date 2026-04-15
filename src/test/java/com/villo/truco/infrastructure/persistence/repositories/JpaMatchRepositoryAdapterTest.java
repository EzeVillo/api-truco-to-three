package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.persistence.entities.MatchJpaEntity;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.mappers.MatchMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    final var joinCodeRegistryRepository = mock(JoinCodeRegistryRepository.class);
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper,
        joinCodeRegistryRepository);

    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)), Visibility.PRIVATE);
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
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper,
        mock(JoinCodeRegistryRepository.class));
    final var match = Match.create(PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)), Visibility.PRIVATE);
    when(mapper.toEntity(match)).thenReturn(new MatchJpaEntity());
    when(springRepo.saveAndFlush(any())).thenThrow(
        new ObjectOptimisticLockingFailureException("Match", match.getId().value()));

    assertThatThrownBy(() -> adapter.save(match)).isInstanceOf(StaleAggregateException.class);
  }

  @Test
  @DisplayName("findPublicWaiting cursor-based delega al repo spring con limit + 1")
  void findPublicWaitingCursorDelegates() {

    final var springRepo = mock(SpringDataMatchRepository.class);
    final var mapper = mock(MatchMapper.class);
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper,
        mock(JoinCodeRegistryRepository.class));
    final var instant = Instant.parse("2026-04-03T12:30:00Z");
    final var id = UUID.fromString("22222222-2222-2222-2222-222222222222");
    final var cursor = new PublicLobbyCursor(instant, id).encode();

    when(springRepo.findPublicWaitingPage(any(), any(), any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, cursor));

    verify(springRepo).findPublicWaitingPage(eq(instant), eq(id),
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

  @Test
  @DisplayName("findPublicWaiting sin cursor usa la query inicial paginada")
  void findPublicWaitingWithoutCursorUsesInitialPageQuery() {

    final var springRepo = mock(SpringDataMatchRepository.class);
    final var mapper = mock(MatchMapper.class);
    final var adapter = new JpaMatchRepositoryAdapter(springRepo, mapper,
        mock(JoinCodeRegistryRepository.class));

    when(springRepo.findInitialPublicWaitingPage(any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, null));

    verify(springRepo).findInitialPublicWaitingPage(
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

}
