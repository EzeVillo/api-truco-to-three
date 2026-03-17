package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.MatchJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataMatchRepository extends JpaRepository<MatchJpaEntity, UUID> {

  Optional<MatchJpaEntity> findByInviteCode(String inviteCode);

  @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MatchJpaEntity m "
      + "WHERE m.status = 'IN_PROGRESS' AND (m.playerOne = :playerId OR m.playerTwo = :playerId)")
  boolean hasActiveMatch(@Param("playerId") UUID playerId);

  @Query("SELECT m.id FROM MatchJpaEntity m "
      + "WHERE m.status IN ('IN_PROGRESS', 'READY') AND m.lastActivityAt < :idleSince")
  List<UUID> findIdleMatchIds(@Param("idleSince") Instant idleSince);

}
