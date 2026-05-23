package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.RematchSessionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRematchSessionRepository extends
    JpaRepository<RematchSessionJpaEntity, UUID> {

  Optional<RematchSessionJpaEntity> findByOriginMatchId(UUID originMatchId);

  @Query("SELECT r FROM RematchSessionJpaEntity r WHERE r.status = 'OPEN' "
      + "AND (r.playerOneId = :playerId OR r.playerTwoId = :playerId)")
  Optional<RematchSessionJpaEntity> findOpenByPlayer(@Param("playerId") UUID playerId);

  @Query("SELECT r FROM RematchSessionJpaEntity r WHERE r.status = 'OPEN' "
      + "AND r.expiresAt <= :now ORDER BY r.expiresAt ASC")
  List<RematchSessionJpaEntity> findExpiredCandidates(@Param("now") Instant now, Pageable pageable);

  @Query("SELECT r.id AS id, r.expiresAt AS expiresAt FROM RematchSessionJpaEntity r WHERE r.status = 'OPEN'")
  List<RematchSessionExpirationProjection> findOpenSessionsWithExpiration();

}
