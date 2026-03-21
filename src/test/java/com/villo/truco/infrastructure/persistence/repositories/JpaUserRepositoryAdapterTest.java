package com.villo.truco.infrastructure.persistence.repositories;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.infrastructure.persistence.mappers.UserMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JpaUserRepositoryAdapter")
class JpaUserRepositoryAdapterTest {

  @Test
  @DisplayName("delega existsByUsername y findByUsername")
  void delegatesQueries() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var adapter = new JpaUserRepositoryAdapter(springRepo, mock(UserMapper.class));

    adapter.existsByUsername("u");
    adapter.findByUsername("u");

    verify(springRepo).existsByUsername("u");
    verify(springRepo).findByUsername("u");
  }

}
