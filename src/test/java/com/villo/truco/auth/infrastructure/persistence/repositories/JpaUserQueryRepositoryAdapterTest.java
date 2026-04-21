package com.villo.truco.auth.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.UserUsernameProjection;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JpaUserQueryRepositoryAdapter")
class JpaUserQueryRepositoryAdapterTest {

  @Test
  @DisplayName("resuelve userId exacto por username")
  void findsUserIdByUsername() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var adapter = new JpaUserQueryRepositoryAdapter(springRepo);
    final var userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final var entity = new UserJpaEntity();
    entity.setId(userId);
    entity.setUsername("juancho");

    when(springRepo.findByUsername("juancho")).thenReturn(Optional.of(entity));

    assertThat(adapter.findUserIdByUsername("juancho")).contains(new PlayerId(userId));
  }

  @Test
  @DisplayName("devuelve usernames por ids")
  void findsUsernamesByIds() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var adapter = new JpaUserQueryRepositoryAdapter(springRepo);
    final var userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final var playerId = new PlayerId(userId);

    when(springRepo.findByIdIn(java.util.List.of(userId))).thenReturn(
        java.util.List.of(new UserUsernameProjection() {
          @Override
          public UUID getId() {

            return userId;
          }

          @Override
          public String getUsername() {

            return "juancho";
          }
        }));

    assertThat(adapter.findUsernamesByIds(Set.of(playerId))).containsEntry(playerId, "juancho");
  }

}
