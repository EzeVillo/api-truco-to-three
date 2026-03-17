package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.TournamentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTournamentRepository extends JpaRepository<TournamentJpaEntity, UUID> {

  Optional<TournamentJpaEntity> findByInviteCode(String inviteCode);

  @Query("SELECT t FROM TournamentJpaEntity t JOIN t.fixtures f WHERE f.matchId = :matchId")
  Optional<TournamentJpaEntity> findByMatchId(@Param("matchId") UUID matchId);

}
