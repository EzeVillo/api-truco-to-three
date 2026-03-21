package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.CupJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCupRepository extends JpaRepository<CupJpaEntity, UUID> {

  Optional<CupJpaEntity> findByInviteCode(String inviteCode);

  @Query("SELECT c FROM CupJpaEntity c JOIN c.bouts b WHERE b.matchId = :matchId")
  Optional<CupJpaEntity> findByMatchId(@Param("matchId") UUID matchId);

  @Query("SELECT c FROM CupJpaEntity c JOIN c.participants p "
      + "WHERE c.status = 'IN_PROGRESS' AND p.playerId = :playerId")
  Optional<CupJpaEntity> findInProgressByPlayer(@Param("playerId") UUID playerId);

}
