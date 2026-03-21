package com.villo.truco.infrastructure.persistence.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.infrastructure.persistence.mappers.LeagueMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataLeagueRepository;
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

}
