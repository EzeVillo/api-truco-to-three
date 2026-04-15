package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.LeagueJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataLeagueRepository extends JpaRepository<LeagueJpaEntity, UUID> {

  @Query("SELECT t FROM LeagueJpaEntity t JOIN t.fixtures f WHERE f.matchId = :matchId")
  Optional<LeagueJpaEntity> findByMatchId(@Param("matchId") UUID matchId);

  @Query("SELECT l FROM LeagueJpaEntity l JOIN l.participants p "
      + "WHERE l.status = 'IN_PROGRESS' AND p.playerId = :playerId")
  Optional<LeagueJpaEntity> findInProgressByPlayer(@Param("playerId") UUID playerId);

  @Query("SELECT l FROM LeagueJpaEntity l JOIN l.participants p "
      + "WHERE l.status IN ('WAITING_FOR_PLAYERS', 'WAITING_FOR_START') AND p.playerId = :playerId")
  Optional<LeagueJpaEntity> findWaitingByPlayer(@Param("playerId") UUID playerId);

  @Query("SELECT l.id FROM LeagueJpaEntity l "
      + "WHERE l.status IN ('WAITING_FOR_PLAYERS', 'WAITING_FOR_START') "
      + "AND l.lastActivityAt < :idleSince")
  List<UUID> findIdleLeagueIds(@Param("idleSince") Instant idleSince);

  @Query("SELECT l FROM LeagueJpaEntity l "
      + "WHERE l.visibility = 'PUBLIC' AND l.status = 'WAITING_FOR_PLAYERS' "
      + "ORDER BY l.lastActivityAt DESC")
  List<LeagueJpaEntity> findPublicWaiting();

  @Query("SELECT l FROM LeagueJpaEntity l "
      + "WHERE l.visibility = 'PUBLIC' AND l.status = 'WAITING_FOR_PLAYERS' "
      + "ORDER BY l.lastActivityAt DESC, l.id DESC")
  List<LeagueJpaEntity> findInitialPublicWaitingPage(Pageable pageable);

  @Query("SELECT l FROM LeagueJpaEntity l "
      + "WHERE l.visibility = 'PUBLIC' AND l.status = 'WAITING_FOR_PLAYERS' "
      + "AND (l.lastActivityAt < :afterActivityAt "
      + "OR (l.lastActivityAt = :afterActivityAt AND l.id < :afterId)) "
      + "ORDER BY l.lastActivityAt DESC, l.id DESC")
  List<LeagueJpaEntity> findPublicWaitingPage(@Param("afterActivityAt") Instant afterActivityAt,
      @Param("afterId") UUID afterId, Pageable pageable);

}
