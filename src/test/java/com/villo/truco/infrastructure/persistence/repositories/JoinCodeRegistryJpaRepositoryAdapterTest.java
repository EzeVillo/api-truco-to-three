package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.exceptions.JoinCodeRegistryCollisionException;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.infrastructure.persistence.entities.JoinCodeRegistryJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JoinCodeRegistryJpaRepositoryAdapter")
class JoinCodeRegistryJpaRepositoryAdapterTest {

  @Test
  @DisplayName("save inserta mapeo cuando el codigo no existe")
  void saveInsertsWhenCodeIsAbsent() {

    final var springRepository = mock(SpringDataJoinCodeRegistryRepository.class);
    final var adapter = new JoinCodeRegistryJpaRepositoryAdapter(springRepository);
    final var registration = new JoinCodeRegistration(JoinCode.of("ABCD1234"), JoinTargetType.MATCH,
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    when(springRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);

    adapter.save(registration);

    verify(springRepository).insertIfAbsent("ABCD1234", "MATCH",
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
  }

  @Test
  @DisplayName("save es idempotente cuando ya existe el mismo mapeo")
  void saveIsIdempotentForSameMapping() {

    final var springRepository = mock(SpringDataJoinCodeRegistryRepository.class);
    final var adapter = new JoinCodeRegistryJpaRepositoryAdapter(springRepository);
    final var registration = new JoinCodeRegistration(JoinCode.of("ABCD1234"),
        JoinTargetType.LEAGUE, UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    final var entity = new JoinCodeRegistryJpaEntity();
    entity.setJoinCode("ABCD1234");
    entity.setTargetType("LEAGUE");
    entity.setTargetId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    when(springRepository.insertIfAbsent(any(), any(), any())).thenReturn(0);
    when(springRepository.findById("ABCD1234")).thenReturn(Optional.of(entity));

    adapter.save(registration);

    verify(springRepository).findById("ABCD1234");
  }

  @Test
  @DisplayName("save falla si el join code ya apunta a otro recurso")
  void saveFailsForCollision() {

    final var springRepository = mock(SpringDataJoinCodeRegistryRepository.class);
    final var adapter = new JoinCodeRegistryJpaRepositoryAdapter(springRepository);
    final var registration = new JoinCodeRegistration(JoinCode.of("ABCD1234"), JoinTargetType.CUP,
        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
    final var entity = new JoinCodeRegistryJpaEntity();
    entity.setJoinCode("ABCD1234");
    entity.setTargetType("MATCH");
    entity.setTargetId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));

    when(springRepository.insertIfAbsent(any(), any(), any())).thenReturn(0);
    when(springRepository.findById("ABCD1234")).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> adapter.save(registration)).isInstanceOf(
        JoinCodeRegistryCollisionException.class);
  }

}
