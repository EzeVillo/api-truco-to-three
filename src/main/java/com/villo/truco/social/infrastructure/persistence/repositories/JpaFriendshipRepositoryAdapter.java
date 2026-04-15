package com.villo.truco.social.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.infrastructure.persistence.mappers.FriendshipMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataFriendshipRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaFriendshipRepositoryAdapter implements FriendshipRepository,
    FriendshipQueryRepository {

  private final SpringDataFriendshipRepository springDataFriendshipRepository;
  private final FriendshipMapper friendshipMapper;

  public JpaFriendshipRepositoryAdapter(
      final SpringDataFriendshipRepository springDataFriendshipRepository,
      final FriendshipMapper friendshipMapper) {

    this.springDataFriendshipRepository = springDataFriendshipRepository;
    this.friendshipMapper = friendshipMapper;
  }

  @Override
  @Transactional
  public void save(final Friendship friendship) {

    try {
      final var entity = this.friendshipMapper.toEntity(friendship);
      this.springDataFriendshipRepository.saveAndFlush(entity);
      friendship.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "Friendship " + friendship.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<Friendship> findById(final FriendshipId friendshipId) {

    return this.springDataFriendshipRepository.findById(friendshipId.value())
        .map(this.friendshipMapper::toDomain);
  }

  @Override
  public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
      final PlayerId secondPlayerId) {

    return this.springDataFriendshipRepository.existsAcceptedByPlayers(firstPlayerId.value(),
        secondPlayerId.value());
  }

  @Override
  public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
      final PlayerId secondPlayerId) {

    return this.springDataFriendshipRepository.findPendingByPlayers(firstPlayerId.value(),
        secondPlayerId.value()).map(this.friendshipMapper::toDomain);
  }

  @Override
  public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
      final PlayerId addresseeId) {

    // Accept/decline/cancel need the original request direction, unlike the symmetric pair lookup.
    return this.springDataFriendshipRepository.findPendingByRequesterAndAddressee(
        requesterId.value(), addresseeId.value()).map(this.friendshipMapper::toDomain);
  }

  @Override
  public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
      final PlayerId secondPlayerId) {

    return this.springDataFriendshipRepository.findAcceptedByPlayers(firstPlayerId.value(),
        secondPlayerId.value()).map(this.friendshipMapper::toDomain);
  }

  @Override
  public List<Friendship> findAcceptedByPlayer(final PlayerId playerId) {

    return this.springDataFriendshipRepository.findAcceptedByPlayer(playerId.value()).stream()
        .map(this.friendshipMapper::toDomain).toList();
  }

  @Override
  public List<Friendship> findPendingReceivedBy(final PlayerId playerId) {

    return this.springDataFriendshipRepository.findByAddresseeIdAndStatusOrderByIdDesc(
        playerId.value(), "PENDING").stream().map(this.friendshipMapper::toDomain).toList();
  }

  @Override
  public List<Friendship> findPendingSentBy(final PlayerId playerId) {

    return this.springDataFriendshipRepository.findByRequesterIdAndStatusOrderByIdDesc(
        playerId.value(), "PENDING").stream().map(this.friendshipMapper::toDomain).toList();
  }

}
