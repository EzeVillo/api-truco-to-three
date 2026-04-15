package com.villo.truco.social.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.infrastructure.persistence.entities.FriendshipJpaEntity;
import com.villo.truco.social.infrastructure.persistence.mappers.FriendshipMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataFriendshipRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DisplayName("JpaFriendshipRepositoryAdapter")
class JpaFriendshipRepositoryAdapterTest {

  @Test
  @DisplayName("save actualiza version del agregado")
  void saveUpdatesVersion() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var friendship = Friendship.request(PlayerId.generate(), PlayerId.generate());
    final var entity = new FriendshipJpaEntity();
    entity.setVersion(5);
    when(mapper.toEntity(friendship)).thenReturn(entity);

    adapter.save(friendship);

    verify(springRepo).saveAndFlush(entity);
    assertThat(friendship.getVersion()).isEqualTo(5);
  }

  @Test
  @DisplayName("save traduce optimistic lock")
  void saveTranslatesOptimisticLock() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var friendship = Friendship.request(PlayerId.generate(), PlayerId.generate());
    when(mapper.toEntity(friendship)).thenReturn(new FriendshipJpaEntity());
    when(springRepo.saveAndFlush(any())).thenThrow(
        new ObjectOptimisticLockingFailureException("Friendship", friendship.getId().value()));

    assertThatThrownBy(() -> adapter.save(friendship)).isInstanceOf(StaleAggregateException.class);
  }

  @Test
  @DisplayName("existsAcceptedByPlayers delega consulta semantica de existencia")
  void existsAcceptedByPlayersDelegatesToExistsQuery() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var firstPlayerId = PlayerId.generate();
    final var secondPlayerId = PlayerId.generate();
    when(springRepo.existsAcceptedByPlayers(firstPlayerId.value(),
        secondPlayerId.value())).thenReturn(true);

    final var exists = adapter.existsAcceptedByPlayers(firstPlayerId, secondPlayerId);

    assertThat(exists).isTrue();
    verify(springRepo).existsAcceptedByPlayers(firstPlayerId.value(), secondPlayerId.value());
  }

  @Test
  @DisplayName("findPendingByPlayers delega query simetrica entre jugadores")
  void findPendingByPlayersDelegatesToUndirectedQuery() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var firstPlayerId = PlayerId.generate();
    final var secondPlayerId = PlayerId.generate();
    final var entity = new FriendshipJpaEntity();
    final var friendship = Friendship.request(firstPlayerId, secondPlayerId);
    when(springRepo.findPendingByPlayers(firstPlayerId.value(), secondPlayerId.value())).thenReturn(
        java.util.Optional.of(entity));
    when(mapper.toDomain(entity)).thenReturn(friendship);

    final var resolved = adapter.findPendingByPlayers(firstPlayerId, secondPlayerId);

    assertThat(resolved).contains(friendship);
    verify(springRepo).findPendingByPlayers(firstPlayerId.value(), secondPlayerId.value());
  }

  @Test
  @DisplayName("findPendingByRequesterAndAddressee delega query dirigida")
  void findPendingByRequesterAndAddresseeDelegatesToDirectedQuery() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var requesterId = PlayerId.generate();
    final var addresseeId = PlayerId.generate();
    final var entity = new FriendshipJpaEntity();
    final var friendship = Friendship.request(requesterId, addresseeId);
    when(springRepo.findPendingByRequesterAndAddressee(requesterId.value(),
        addresseeId.value())).thenReturn(java.util.Optional.of(entity));
    when(mapper.toDomain(entity)).thenReturn(friendship);

    final var resolved = adapter.findPendingByRequesterAndAddressee(requesterId, addresseeId);

    assertThat(resolved).contains(friendship);
    verify(springRepo).findPendingByRequesterAndAddressee(requesterId.value(), addresseeId.value());
  }

  @Test
  @DisplayName("findAcceptedByPlayers delega query explicita de amistad aceptada")
  void findAcceptedByPlayersDelegatesToAcceptedQuery() {

    final var springRepo = mock(SpringDataFriendshipRepository.class);
    final var mapper = mock(FriendshipMapper.class);
    final var adapter = new JpaFriendshipRepositoryAdapter(springRepo, mapper);
    final var firstPlayerId = PlayerId.generate();
    final var secondPlayerId = PlayerId.generate();
    final var entity = new FriendshipJpaEntity();
    final var friendship = Friendship.request(firstPlayerId, secondPlayerId);
    friendship.accept(secondPlayerId);
    when(
        springRepo.findAcceptedByPlayers(firstPlayerId.value(), secondPlayerId.value())).thenReturn(
        Optional.of(entity));
    when(mapper.toDomain(entity)).thenReturn(friendship);

    final var resolved = adapter.findAcceptedByPlayers(firstPlayerId, secondPlayerId);

    assertThat(resolved).contains(friendship);
    verify(springRepo).findAcceptedByPlayers(firstPlayerId.value(), secondPlayerId.value());
  }

}
