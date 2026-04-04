package com.villo.truco.infrastructure.persistence.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.infrastructure.persistence.mappers.LeagueMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataLeagueRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JpaLeagueRepositoryAdapter")
class JpaLeagueRepositoryAdapterTest {

  @Test
  @DisplayName("findById delega en repositorio Spring")
  void findByIdDelegates() {

    final var springRepo = mock(SpringDataLeagueRepository.class);
    final var adapter = new JpaLeagueRepositoryAdapter(springRepo, mock(LeagueMapper.class));

    adapter.findById(LeagueId.generate());

    verify(springRepo).findById(any());
  }

  @Test
  @DisplayName("findPublicWaiting cursor-based delega al repo spring con limit + 1")
  void findPublicWaitingCursorDelegates() {

    final var springRepo = mock(SpringDataLeagueRepository.class);
    final var adapter = new JpaLeagueRepositoryAdapter(springRepo, mock(LeagueMapper.class));
    final var instant = Instant.parse("2026-04-03T12:30:00Z");
    final var id = UUID.fromString("33333333-3333-3333-3333-333333333333");
    final var cursor = new PublicLobbyCursor(instant, id).encode();

    when(springRepo.findPublicWaitingPage(any(), any(), any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, cursor));

    verify(springRepo).findPublicWaitingPage(eq(instant), eq(id),
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

  @Test
  @DisplayName("findPublicWaiting sin cursor usa la query inicial paginada")
  void findPublicWaitingWithoutCursorUsesInitialPageQuery() {

    final var springRepo = mock(SpringDataLeagueRepository.class);
    final var adapter = new JpaLeagueRepositoryAdapter(springRepo, mock(LeagueMapper.class));

    when(springRepo.findInitialPublicWaitingPage(any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, null));

    verify(springRepo).findInitialPublicWaitingPage(
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

}
