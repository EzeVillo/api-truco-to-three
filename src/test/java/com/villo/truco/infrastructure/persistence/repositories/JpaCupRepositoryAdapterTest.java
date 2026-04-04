package com.villo.truco.infrastructure.persistence.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.infrastructure.persistence.mappers.CupMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataCupRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JpaCupRepositoryAdapter")
class JpaCupRepositoryAdapterTest {

  @Test
  @DisplayName("findById delega en repositorio Spring")
  void findByIdDelegates() {

    final var springRepo = mock(SpringDataCupRepository.class);
    final var adapter = new JpaCupRepositoryAdapter(springRepo, mock(CupMapper.class));

    adapter.findById(com.villo.truco.domain.model.cup.valueobjects.CupId.generate());

    verify(springRepo).findById(any());
  }

  @Test
  @DisplayName("findPublicWaiting cursor-based delega al repo spring con limit + 1")
  void findPublicWaitingCursorDelegates() {

    final var springRepo = mock(SpringDataCupRepository.class);
    final var adapter = new JpaCupRepositoryAdapter(springRepo, mock(CupMapper.class));
    final var instant = Instant.parse("2026-04-03T12:30:00Z");
    final var id = UUID.fromString("44444444-4444-4444-4444-444444444444");
    final var cursor = new PublicLobbyCursor(instant, id).encode();

    when(springRepo.findPublicWaitingPage(any(), any(), any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, cursor));

    verify(springRepo).findPublicWaitingPage(eq(instant), eq(id),
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

  @Test
  @DisplayName("findPublicWaiting sin cursor usa la query inicial paginada")
  void findPublicWaitingWithoutCursorUsesInitialPageQuery() {

    final var springRepo = mock(SpringDataCupRepository.class);
    final var adapter = new JpaCupRepositoryAdapter(springRepo, mock(CupMapper.class));

    when(springRepo.findInitialPublicWaitingPage(any())).thenReturn(List.of());

    adapter.findPublicWaiting(new CursorPageQuery(20, null));

    verify(springRepo).findInitialPublicWaitingPage(
        argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 21));
  }

}
