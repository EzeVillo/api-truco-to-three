package com.villo.truco.social.infrastructure.persistence.repositories.spring;

import com.villo.truco.social.infrastructure.persistence.entities.FriendshipJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataFriendshipRepository extends JpaRepository<FriendshipJpaEntity, UUID> {

  @Query(value = """
      select exists(
        select 1
        from social_friendships f
        where ((f.requester_id = :firstPlayerId and f.addressee_id = :secondPlayerId)
          or (f.requester_id = :secondPlayerId and f.addressee_id = :firstPlayerId))
          and f.status = 'ACCEPTED'
      )
      """, nativeQuery = true)
  boolean existsAcceptedByPlayers(@Param("firstPlayerId") UUID firstPlayerId,
      @Param("secondPlayerId") UUID secondPlayerId);

  @Query("""
      select f
      from FriendshipJpaEntity f
      where ((f.requesterId = :firstPlayerId and f.addresseeId = :secondPlayerId)
        or (f.requesterId = :secondPlayerId and f.addresseeId = :firstPlayerId))
        and f.status = 'PENDING'
      """)
  Optional<FriendshipJpaEntity> findPendingByPlayers(@Param("firstPlayerId") UUID firstPlayerId,
      @Param("secondPlayerId") UUID secondPlayerId);

  @Query("""
      select f
      from FriendshipJpaEntity f
      where f.requesterId = :requesterId
        and f.addresseeId = :addresseeId
        and f.status = 'PENDING'
      """)
  Optional<FriendshipJpaEntity> findPendingByRequesterAndAddressee(
      @Param("requesterId") UUID requesterId, @Param("addresseeId") UUID addresseeId);

  @Query("""
      select f
      from FriendshipJpaEntity f
      where ((f.requesterId = :firstPlayerId and f.addresseeId = :secondPlayerId)
        or (f.requesterId = :secondPlayerId and f.addresseeId = :firstPlayerId))
        and f.status = 'ACCEPTED'
      """)
  Optional<FriendshipJpaEntity> findAcceptedByPlayers(@Param("firstPlayerId") UUID firstPlayerId,
      @Param("secondPlayerId") UUID secondPlayerId);

  @Query("""
      select f
      from FriendshipJpaEntity f
      where f.status = 'ACCEPTED'
        and (f.requesterId = :playerId or f.addresseeId = :playerId)
      """)
  List<FriendshipJpaEntity> findAcceptedByPlayer(@Param("playerId") UUID playerId);

  List<FriendshipJpaEntity> findByAddresseeIdAndStatusOrderByIdDesc(UUID addresseeId,
      String status);

  List<FriendshipJpaEntity> findByRequesterIdAndStatusOrderByIdDesc(UUID requesterId,
      String status);

}
