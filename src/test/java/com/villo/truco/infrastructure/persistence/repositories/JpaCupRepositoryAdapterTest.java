package com.villo.truco.infrastructure.persistence.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.infrastructure.persistence.mappers.CupMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataCupRepository;
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

}
